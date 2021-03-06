package org.porting.less4j.core.parser;

import java.util.List;

import org.porting.less4j.core.ast.Selector;
import org.porting.less4j.core.ast.SelectorCombinator;
import org.porting.less4j.core.ast.SimpleSelector;

public class SelectorBuilder {

  private SimpleSelector currentSimpleSelector;
  private Selector result;
  private Selector currentSelector;
  private final HiddenTokenAwareTree token;
  
  private final ASTBuilderSwitch parentBuilder;
  private HiddenTokenAwareTree lastCombinator;

  public SelectorBuilder(HiddenTokenAwareTree token, ASTBuilderSwitch parentBuilder) {
    this.token = token;
    this.parentBuilder = parentBuilder;
  }

  public Selector buildSelector() {
    List<HiddenTokenAwareTree> members = token.getChildren();
    HiddenTokenAwareTree previousNonCombinator = null;
    for (HiddenTokenAwareTree kid : members) {
      switch (kid.getType()) {
      case LessLexer.ELEMENT_NAME:
        addElementName(kid);
        previousNonCombinator = kid;
        break;
      case LessLexer.ELEMENT_SUBSEQUENT:
        addElementSubsequent(previousNonCombinator, kid);
        previousNonCombinator = kid;
        break;
      default:
        lastCombinator = kid;
      }
      
    }
    return result;
  }

  private void addElementSubsequent(HiddenTokenAwareTree previousNonCombinator, HiddenTokenAwareTree kid) {
    if (previousNonCombinator==null) {
      addWithImplicitStar(kid);
      return ;
    }
    if (previousNonCombinator.getTokenStopIndex() + 1 < kid.getTokenStartIndex()) {
      addWithImplicitStar(kid);
      return ;
    }
    //finally, add subsequent element to the previous simple selector
    addSubsequent(kid);
  }

  public void addSubsequent(HiddenTokenAwareTree kid) {
    currentSimpleSelector.addSubsequent(parentBuilder.switchOn(kid.getChild(0)));
  }

  private void addWithImplicitStar(HiddenTokenAwareTree kid) {
    currentSimpleSelector = new SimpleSelector(kid, null, true);
    currentSimpleSelector.setEmptyForm(true);
    addSubsequent(kid);
    startNewSelector();
    currentSelector.setHead(currentSimpleSelector);
  }

  private void addElementName(HiddenTokenAwareTree kid) {
    HiddenTokenAwareTree realName = kid.getChild(0);
    currentSimpleSelector = new SimpleSelector(kid, realName.getText(), realName.getType()==LessLexer.STAR);
    startNewSelector();
    currentSelector.setHead(currentSimpleSelector);
  }

  private void addSelectorCombinator(HiddenTokenAwareTree kid) {
    currentSelector.setCombinator(new SelectorCombinator(kid, toSelectorCombinator(kid)));
  }

  private SelectorCombinator.Combinator toSelectorCombinator(HiddenTokenAwareTree token) {
    switch (token.getType()) {
    case LessLexer.PLUS:
      return SelectorCombinator.Combinator.ADJACENT_SIBLING;
    case LessLexer.GREATER:
      return SelectorCombinator.Combinator.CHILD;
    case LessLexer.TILDE:
      return SelectorCombinator.Combinator.GENERAL_SIBLING;
    case LessLexer.EMPTY_COMBINATOR:
      return SelectorCombinator.Combinator.DESCENDANT;
    }

    throw new IllegalStateException("Unknown: " + token.getType());
  }

  private void startNewSelector() {
    Selector newSelector = new Selector(token);
    if (currentSelector!=null) {
      addSelectorCombinator(lastCombinator);
      currentSelector.setRight(newSelector);
    }
    
    currentSelector = newSelector;
    
    if (result==null)
      result = currentSelector;
  }

}
