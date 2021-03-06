package org.porting.less4j.core.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.porting.less4j.core.ast.ASTCssNode;
import org.porting.less4j.core.ast.ASTCssNodeType;
import org.porting.less4j.core.ast.CharsetDeclaration;
import org.porting.less4j.core.ast.ComposedExpression;
import org.porting.less4j.core.ast.CssClass;
import org.porting.less4j.core.ast.Declaration;
import org.porting.less4j.core.ast.Expression;
import org.porting.less4j.core.ast.ExpressionOperator;
import org.porting.less4j.core.ast.FontFace;
import org.porting.less4j.core.ast.IdSelector;
import org.porting.less4j.core.ast.IdentifierExpression;
import org.porting.less4j.core.ast.IndirectVariable;
import org.porting.less4j.core.ast.Media;
import org.porting.less4j.core.ast.MediaExpression;
import org.porting.less4j.core.ast.MediaExpressionFeature;
import org.porting.less4j.core.ast.MediaQuery;
import org.porting.less4j.core.ast.Medium;
import org.porting.less4j.core.ast.MediumModifier;
import org.porting.less4j.core.ast.MediumType;
import org.porting.less4j.core.ast.Nth;
import org.porting.less4j.core.ast.Nth.Form;
import org.porting.less4j.core.ast.NumberExpression;
import org.porting.less4j.core.ast.Pseudo;
import org.porting.less4j.core.ast.PseudoClass;
import org.porting.less4j.core.ast.PseudoElement;
import org.porting.less4j.core.ast.RuleSet;
import org.porting.less4j.core.ast.RuleSetsBody;
import org.porting.less4j.core.ast.Selector;
import org.porting.less4j.core.ast.SelectorAttribute;
import org.porting.less4j.core.ast.SelectorOperator;
import org.porting.less4j.core.ast.SignedExpression;
import org.porting.less4j.core.ast.StyleSheet;
import org.porting.less4j.core.ast.Variable;
import org.porting.less4j.core.ast.VariableDeclaration;

class ASTBuilderSwitch extends TokenTypeSwitch<ASTCssNode> {

  private final TermBuilder termBuilder = new TermBuilder(this);
  // as stated here: http://www.w3.org/TR/css3-selectors/#pseudo-elements
  private static Set<String> COLONLESS_PSEUDOELEMENTS = new HashSet<String>();
  static {
    COLONLESS_PSEUDOELEMENTS.add("first-line");
    COLONLESS_PSEUDOELEMENTS.add("first-letter");
    COLONLESS_PSEUDOELEMENTS.add("before");
    COLONLESS_PSEUDOELEMENTS.add("after");
  }

  public StyleSheet handleStyleSheet(HiddenTokenAwareTree token) {
    StyleSheet result = new StyleSheet(token);
    if (token.getChildren() == null || token.getChildren().isEmpty())
      return result;

    for (HiddenTokenAwareTree kid : token.getChildren()) {
      result.addMember(switchOn(kid));
    }

    return result;
  }

  public Expression handleTerm(HiddenTokenAwareTree token) {
    return termBuilder.buildFromTerm(token);
  }

  public Expression handleExpression(HiddenTokenAwareTree token) {
    LinkedList<HiddenTokenAwareTree> children = new LinkedList<HiddenTokenAwareTree>(token.getChildren());
    if (children.size() == 0)
      throw new IncorrectTreeException(token);

    if (children.size() == 1) {
      Expression head = (Expression) switchOn(children.get(0));
      // we have to switch to parent token, because we would loose it otherwise.
      // for example, comments before simple expressions would not be accessible
      // anymore
      head.setUnderlyingStructure(token);
      return head;
    }

    return createExpression(token, children);
  }

  private Expression createExpression(HiddenTokenAwareTree parent, LinkedList<HiddenTokenAwareTree> members) {
    // this must represent a term. Otherwise we are doomed anyway.
    Expression head = (Expression) switchOn(members.removeFirst());
    while (!members.isEmpty()) {
      ExpressionOperator operator = readExpressionOperator(members);
      if (members.isEmpty())
        return new ComposedExpression(parent, head, operator, null);

      Expression next = (Expression) switchOn(members.removeFirst());
      head = new ComposedExpression(parent, head, operator, next);
    }
    return head;
  }

  public ExpressionOperator readExpressionOperator(LinkedList<HiddenTokenAwareTree> members) {
    HiddenTokenAwareTree token = members.removeFirst();
    ExpressionOperator operator = new ExpressionOperator(token, toExpressionOperator(token));
    return operator;
  }

  private ExpressionOperator.Operator toExpressionOperator(HiddenTokenAwareTree token) {
    switch (token.getType()) {
    case LessLexer.SOLIDUS:
      return ExpressionOperator.Operator.SOLIDUS;

    case LessLexer.COMMA:
      return ExpressionOperator.Operator.COMMA;

    case LessLexer.STAR:
      return ExpressionOperator.Operator.STAR;

    case LessLexer.MINUS:
      return ExpressionOperator.Operator.MINUS;

    case LessLexer.PLUS:
      return ExpressionOperator.Operator.PLUS;

    case LessLexer.EMPTY_SEPARATOR:
      return ExpressionOperator.Operator.EMPTY_OPERATOR;

    default:
      break;
    }

    throw new IncorrectTreeException("Unknown operator type. ", token);
  }

  public Variable handleVariable(HiddenTokenAwareTree token) {
    return termBuilder.buildFromVariable(token);
  }

  public IndirectVariable handleIndirectVariable(HiddenTokenAwareTree token) {
    return termBuilder.buildFromIndirectVariable(token);
  }

  public Declaration handleDeclaration(HiddenTokenAwareTree token) {
    List<HiddenTokenAwareTree> children = token.getChildren();

    String name = children.get(0).getText();
    if (children.size() == 1)
      return new Declaration(token, name);

    HiddenTokenAwareTree expressionToken = children.get(1);
    if (expressionToken.getType() == LessLexer.IMPORTANT_SYM)
      return new Declaration(token, name, null, true);

    Expression expression = (Expression) switchOn(expressionToken);
    if (children.size() == 2)
      return new Declaration(token, name, expression);

    if (children.get(2).getType() == LessLexer.IMPORTANT_SYM)
      return new Declaration(token, name, expression, true);

    throw new IncorrectTreeException(token);
  }

  public FontFace handleFontFace(HiddenTokenAwareTree token) {
    FontFace result = new FontFace(token);

    List<HiddenTokenAwareTree> children = token.getChildren();
    List<Declaration> declarations = new ArrayList<Declaration>();
    for (HiddenTokenAwareTree kid : children) {
      if (kid.getType() == LessLexer.DECLARATION)
        declarations.add(handleDeclaration(kid));
    }

    result.addMembers(declarations);
    return result;
  }

  public CharsetDeclaration handleCharsetDeclaration(HiddenTokenAwareTree token) {
    List<HiddenTokenAwareTree> children = token.getChildren();
    if (children.isEmpty())
      throw new IncorrectTreeException(token);

    return new CharsetDeclaration(token, children.get(0).getText());
  }

  public RuleSet handleRuleSet(HiddenTokenAwareTree token) {
    RuleSet ruleSet = new RuleSet(token);

    List<Selector> selectors = new ArrayList<Selector>();
    List<HiddenTokenAwareTree> children = token.getChildren();

    ASTCssNode previousKid = null;
    for (HiddenTokenAwareTree kid : children) {
      if (kid.getType() == LessLexer.SELECTOR) {
        Selector selector = handleSelector(kid);
        selectors.add(selector);
        previousKid = selector;
      }
      if (kid.getType() == LessLexer.BODY) {
        RuleSetsBody body = handleRuleSetsBody(kid);
        ruleSet.setBody(body);
        previousKid = body;
      }
      if (kid.getType() == LessLexer.COMMA) {
        previousKid.getUnderlyingStructure().addFollowing(kid.getPreceding());
      }
    }

    ruleSet.addSelectors(selectors);
    return ruleSet;
  }

  public RuleSetsBody handleRuleSetsBody(HiddenTokenAwareTree token) {
    if (token.getChildren() == null)
      return new RuleSetsBody(token);

    List<ASTCssNode> members = new ArrayList<ASTCssNode>();
    for (HiddenTokenAwareTree kid : token.getChildren()) {
      members.add(switchOn(kid));
    }

    return new RuleSetsBody(token, members);
  }

  public Selector handleSelector(HiddenTokenAwareTree token) {
    SelectorBuilder builder = new SelectorBuilder(token, this);
    return builder.buildSelector();
  }

  public CssClass handleCssClass(HiddenTokenAwareTree token) {
    List<HiddenTokenAwareTree> children = token.getChildren();
    HiddenTokenAwareTree nameToken = children.get(0);

    String name = nameToken.getText();
    if (nameToken.getType() != LessLexer.IDENT && name.length() > 1) {
      name = name.substring(1, name.length());
    }

    CssClass result = new CssClass(token, name);
    return result;
  }

  public SelectorAttribute handleSelectorAttribute(HiddenTokenAwareTree token) {
    List<HiddenTokenAwareTree> children = token.getChildren();
    if (children.size() == 0)
      throw new IncorrectTreeException(token);

    if (children.size() == 1)
      return new SelectorAttribute(token, children.get(0).getText());

    if (children.size() < 3)
      throw new IncorrectTreeException(token);

    return new SelectorAttribute(token, children.get(0).getText(), handleSelectorOperator(children.get(1)), children.get(2).getText());
  }

  public SelectorOperator handleSelectorOperator(HiddenTokenAwareTree token) {
    return new SelectorOperator(token, toSelectorOperator(token));
  }

  private SelectorOperator.Operator toSelectorOperator(HiddenTokenAwareTree token) {
    switch (token.getType()) {
    case LessLexer.OPEQ:
      return SelectorOperator.Operator.EQUALS;

    case LessLexer.INCLUDES:
      return SelectorOperator.Operator.INCLUDES;

    case LessLexer.DASHMATCH:
      return SelectorOperator.Operator.SPECIAL_PREFIX;

    case LessLexer.PREFIXMATCH:
      return SelectorOperator.Operator.PREFIXMATCH;

    case LessLexer.SUFFIXMATCH:
      return SelectorOperator.Operator.SUFFIXMATCH;

    case LessLexer.SUBSTRINGMATCH:
      return SelectorOperator.Operator.SUBSTRINGMATCH;

    default:
      break;
    }

    throw new IncorrectTreeException(token);
  }

  public Pseudo handlePseudo(HiddenTokenAwareTree token) {
    List<HiddenTokenAwareTree> children = token.getChildren();
    if (children.size() == 0 || children.size() == 1)
      throw new IncorrectTreeException(token);

    // the child number 0 is a :
    HiddenTokenAwareTree t = children.get(1);
    if (t.getType() == LessLexer.COLON) {
      return createPseudoElement(token, 2, false);
    }

    if (COLONLESS_PSEUDOELEMENTS.contains(t.getText().toLowerCase())) {
      return createPseudoElement(token, 1, true);
    }

    if (children.size() == 2)
      return new PseudoClass(token, children.get(1).getText());

    if (children.size() == 3) {
      HiddenTokenAwareTree parameter = children.get(2);
      if (parameter.getType() == LessLexer.NUMBER)
        return new PseudoClass(token, children.get(1).getText(), new NumberExpression(parameter, parameter.getText()));
      if (parameter.getType() == LessLexer.IDENT)
        return new PseudoClass(token, children.get(1).getText(), new IdentifierExpression(parameter, parameter.getText()));

      return new PseudoClass(token, children.get(1).getText(), switchOn(parameter));
    }

    throw new IncorrectTreeException(token);
  }

  public Nth handleNth(HiddenTokenAwareTree token) {
    Expression first = null;
    Expression second = null;
    if (hasChildren(token.getChild(0))) {
      first = termBuilder.buildFromTerm(token.getChild(0));
      String sign = "";
      if (first.getType() == ASTCssNodeType.NEGATED_EXPRESSION) {
        SignedExpression negated = (SignedExpression) first;
        first = negated.getExpression();
        sign = negated.getSign().toSymbol();
      }
      if (first.getType() == ASTCssNodeType.IDENTIFIER_EXPRESSION) {
        IdentifierExpression ident = (IdentifierExpression) first;
        String lowerCaseValue = ident.getValue().toLowerCase();
        lowerCaseValue = sign + lowerCaseValue;
        if ("even".equals(lowerCaseValue)) {
          return new Nth(token, null, null, Form.EVEN);
        } else if ("odd".equals(lowerCaseValue)) {
          return new Nth(token, null, null, Form.ODD);
        } else if ("n".equals(lowerCaseValue) || "-n".equals(lowerCaseValue) || "+n".equals(lowerCaseValue)) {
          first = new NumberExpression(token.getChild(0), lowerCaseValue, NumberExpression.Dimension.REPEATER);
        } else
          throw new IllegalStateException("Unexpected identifier value for nth: " + ident.getValue());
      }
    }

    if (token.getChild(1) != null && hasChildren(token.getChild(1))) {
      second = termBuilder.buildFromTerm(token.getChild(1));
    }

    return new Nth(token, (NumberExpression) first, (NumberExpression) second);
  }

  private boolean hasChildren(HiddenTokenAwareTree token) {
    return token.getChildren() != null && !token.getChildren().isEmpty();
  }

  private PseudoElement createPseudoElement(HiddenTokenAwareTree token, int startIndex, boolean level12Form) {
    List<HiddenTokenAwareTree> children = token.getChildren();
    String name = children.get(startIndex).getText();
    return new PseudoElement(token, name, level12Form);
  }

  public IdSelector handleIdSelector(HiddenTokenAwareTree token) {
    List<HiddenTokenAwareTree> children = token.getChildren();
    if (children.size() != 1)
      throw new IncorrectTreeException(token);

    String text = children.get(0).getText();
    if (text == null || text.length() < 1)
      throw new IncorrectTreeException(token);

    return new IdSelector(token, text.substring(1));
  }

  public Media handleMedia(HiddenTokenAwareTree token) {
    List<HiddenTokenAwareTree> originalChildren = token.getChildren();
    List<HiddenTokenAwareTree> children = new ArrayList<HiddenTokenAwareTree>(originalChildren);
    HiddenTokenAwareTree lbrace = children.remove(1);
    children.get(0).addFollowing(lbrace.getPreceding());
    children.get(1).addBeforePreceding(lbrace.getFollowing());

    HiddenTokenAwareTree rbrace = children.remove(children.size() - 1);
    children.get(children.size() - 1).addFollowing(rbrace.getPreceding());
    rbrace.getParent().addBeforeFollowing(rbrace.getFollowing());

    Media result = new Media(token);
    for (HiddenTokenAwareTree kid : children) {
      if (kid.getType() == LessLexer.MEDIUM_DECLARATION) {
        kid.pushHiddenToKids();
        handleMediaDeclaration(result, kid);
      } else {
        result.addChild(switchOn(kid));
      }

    }
    return result;
  }

  private void handleMediaDeclaration(Media result, HiddenTokenAwareTree declaration) {
    List<HiddenTokenAwareTree> children = declaration.getChildren();

    ASTCssNode previousKid = null;
    for (HiddenTokenAwareTree kid : children) {
      if (kid.getType() == LessLexer.COMMA) {
        previousKid.getUnderlyingStructure().addFollowing(kid.getPreceding());
      } else {
        previousKid = switchOn(kid);
        result.addChild(previousKid);
      }
    }
  }

  public MediaQuery handleMediaQuery(HiddenTokenAwareTree token) {
    MediaQuery result = new MediaQuery(token);
    List<HiddenTokenAwareTree> originalChildren = token.getChildren();

    //each AND identifier may hold preceding comments must be pushed to other tokens
    LinkedList<HiddenTokenAwareTree> children = new LinkedList<HiddenTokenAwareTree>();
    for (HiddenTokenAwareTree kid : originalChildren) {
      if (kid.getType() == LessLexer.IDENT) {
        HiddenTokenAwareTree lastKid = children.peekLast();
        if (lastKid != null) {
          lastKid.addFollowing(kid.getPreceding());
        }
      } else {
        children.add(kid);
      }
    }
    // we have three types of children:
    // * MEDIUM_TYPE
    // * identifier AND whose only function is to hold comments
    // * MEDIA_EXPRESSION
    for (HiddenTokenAwareTree kid : children) {
      if (kid.getType() != LessLexer.IDENT) {
        result.addMember(switchOn(kid));
      } else {
        //we have to copy comments from the AND identifier to surrounding elements.
      }
    }

    return result;
  }

  public Medium handleMedium(HiddenTokenAwareTree token) {
    List<HiddenTokenAwareTree> children = token.getChildren();
    if (children.size() == 1) {
      HiddenTokenAwareTree type = children.get(0);
      return new Medium(token, new MediumModifier(type), new MediumType(type, type.getText()));
    }

    HiddenTokenAwareTree type = children.get(1);
    return new Medium(token, toMediumModifier(children.get(0)), new MediumType(type, type.getText()));
  }

  public MediaExpression handleMediaExpression(HiddenTokenAwareTree token) {
    List<HiddenTokenAwareTree> children = token.getChildren();
    HiddenTokenAwareTree featureNode = children.get(0);
    if (children.size() == 1)
      return new MediaExpression(token, new MediaExpressionFeature(featureNode, featureNode.getText()), null);

    if (children.size() == 2)
      throw new IncorrectTreeException(token);

    HiddenTokenAwareTree colonNode = children.get(1);
    featureNode.addFollowing(colonNode.getPreceding());

    HiddenTokenAwareTree expressionNode = children.get(2);
    Expression expression = (Expression) switchOn(expressionNode);
    return new MediaExpression(token, new MediaExpressionFeature(featureNode, featureNode.getText()), expression);
  }

  private MediumModifier toMediumModifier(HiddenTokenAwareTree token) {
    String modifier = token.getText().toLowerCase();

    if ("not".equals(modifier))
      return new MediumModifier(token, MediumModifier.Modifier.NOT);

    if ("only".equals(modifier))
      return new MediumModifier(token, MediumModifier.Modifier.ONLY);

    throw new IllegalStateException("Unexpected medium modifier: " + modifier);
  }

  public VariableDeclaration handleVariableDeclaration(HiddenTokenAwareTree token) {
    List<HiddenTokenAwareTree> children = token.getChildren();
    HiddenTokenAwareTree name = children.get(0);
    HiddenTokenAwareTree colon = children.get(1);
    HiddenTokenAwareTree expression = children.get(2);
    HiddenTokenAwareTree semi = children.get(3);

    colon.giveHidden(name, expression);
    semi.giveHidden(expression, null);
    token.addBeforeFollowing(semi.getFollowing());

    return new VariableDeclaration(token, new Variable(name, name.getText()), (Expression) switchOn(expression));
  }

}

@SuppressWarnings("serial")
class IncorrectTreeException extends RuntimeException {

  private final HiddenTokenAwareTree node;

  public IncorrectTreeException(HiddenTokenAwareTree node) {
    this.node = node;
  }

  public IncorrectTreeException(String message, HiddenTokenAwareTree node) {
    super(message);
    this.node = node;
  }

  @Override
  public String getMessage() {
    return super.getMessage() + getPositionInformation();
  }

  private String getPositionInformation() {
    if (node == null)
      return "";

    String result = "\n Line: " + node.getLine() + " Character: " + node.getCharPositionInLine();
    return result;
  }
}