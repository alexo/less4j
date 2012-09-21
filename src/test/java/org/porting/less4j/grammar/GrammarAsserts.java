package org.porting.less4j.grammar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.List;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.TreeVisitor;
import org.antlr.runtime.tree.TreeVisitorAction;
import org.porting.less4j.core.parser.ANTLRParser;
import org.porting.less4j.core.parser.HiddenTokenAwareErrorTree;
import org.porting.less4j.core.parser.LessLexer;
import org.porting.less4j.utils.PrintUtils;

public class GrammarAsserts {

  public static void assertValidSelector(final ANTLRParser.ParseResult result) {
    assertValid(result);
    assertEquals(LessLexer.SELECTOR, result.getTree().getType());
  }

  public static void assertValidExpression(final ANTLRParser.ParseResult result) {
    assertValid(result);
    assertEquals(LessLexer.EXPRESSION, result.getTree().getType());
  }

  public static void assertValidTerm(final ANTLRParser.ParseResult result) {
    assertValid(result);
    assertEquals(LessLexer.TERM, result.getTree().getType());
  }

  public static void assertValid(final ANTLRParser.ParseResult result) {
    assertTrue(result.getErrors().isEmpty());
  }

  @SuppressWarnings("rawtypes")
  public static void assertChilds(final CommonTree tree, final int... childType) {
    Iterator kids = tree.getChildren().iterator();
    for (int type : childType) {
      if (!kids.hasNext())
        fail("Some children are missing.");

      CommonTree kid = (CommonTree) kids.next();
      assertEquals("Should be: " + PrintUtils.toName(type)+" was: "+ PrintUtils.toName(kid.getType()), type, kid.getType());
    }

  }

  public static void assertNoTokenMissing(final String crashingSelector, final CommonTree tree) {
    assertNoTokenMissing(crashingSelector, tree, 0);
  }

  public static void assertNoTokenMissing(final String crashingSelector, final CommonTree tree, final int expectedDummies) {
    CommonTokenStream tokens = createTokenStream(crashingSelector);
    int onChannelTokens = countOnChannelTokes(tokens);
    int treeTokesCount = countAllTreeTokens(tokens, tree);
    assertEquals(onChannelTokens + expectedDummies, treeTokesCount);
  }

  private static int countOnChannelTokes(final CommonTokenStream tokens) {
    int numberOfOnChannelTokens = tokens.getNumberOfOnChannelTokens();
    //the above number includes also EOF, we substract that
    return numberOfOnChannelTokens - 1;
  }

  private static int countAllTreeTokens(final CommonTokenStream tokens, final CommonTree tree) {
    CountNodesAction action = new CountNodesAction(tokens);
    TreeVisitor visitor = new TreeVisitor();
    visitor.visit(tree, action);
    return action.getCount();
  }

  private static CommonTokenStream createTokenStream(final String text) {
    ANTLRStringStream input = new ANTLRStringStream(text);
    LessLexer lexer = new LessLexer(input);
    return new CommonTokenStream(lexer);
  }

}

class CountNodesAction implements TreeVisitorAction {

  private int count = 0;
  private final CommonTokenStream tokens;
  
  public CountNodesAction(final CommonTokenStream tokens) {
    super();
    this.tokens = tokens;
  }

  @Override
  public Object pre(final Object t) {
    if (t instanceof HiddenTokenAwareErrorTree) {
      HiddenTokenAwareErrorTree errorNode = (HiddenTokenAwareErrorTree)t;
      int startIndex = errorNode.getStart().getTokenIndex();
      int stopIndex = errorNode.getStop().getTokenIndex();
      int errorTokens = countOnChannelTokes(startIndex, stopIndex);
      count+=errorTokens;
      System.out.println("Error tokens " + errorTokens);
    } else {
      if (!isDummy(((CommonTree)t).getToken())) {
        count++;
//        String string = DebugPrint.toString(count, ((CommonTree)t).getToken());
//        System.out.println(string);
      }
    }
    return t;
  }

  public int getCount() {
    return count;
  }

  @Override
  public Object post(final Object t) {
    return t;
  }

  private int countOnChannelTokes(final int start, final int end) {
    @SuppressWarnings("unchecked")
    List<CommonToken> list = tokens.get(start, end);
    int count = 0;
    for (CommonToken token : list) {
      if (isOnChannel(token) && !isDummy(token))
        count++;
    }
    return count;
  }

  private boolean isDummy(final Token token) {
    return token.getType()<LessLexer.CHARSET_SYM;
  }

  private boolean isOnChannel(final CommonToken token) {
    return token.getChannel()==Token.DEFAULT_CHANNEL;
  }

}
