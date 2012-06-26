package org.porting.less4j.core.ast;

import org.antlr.runtime.tree.CommonTree;

public class Selector extends ASTCssNode {
  
  private final Combinator combinator;
  private final SimpleSelector head;
  private final Selector right;

  public Selector(CommonTree token, SimpleSelector head, Combinator combinator, Selector right) {
    super(token);
    this.head = head;
    this.combinator = combinator;
    this.right = right;
  }

  public boolean isCombined() {
    return combinator!=null;
  }
  
  public Combinator getCombinator() {
    return combinator;
  }

  public SimpleSelector getHead() {
    return head;
  }

  public Selector getRight() {
    return right;
  }

  public ASTCssNodeType getType() {
    return ASTCssNodeType.SELECTOR;
  }
  
  public enum Combinator {
    PLUS, GREATER, EMPTY;
  }

}
