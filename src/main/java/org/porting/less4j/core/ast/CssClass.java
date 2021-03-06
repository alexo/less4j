package org.porting.less4j.core.ast;

import java.util.Collections;
import java.util.List;

import org.porting.less4j.core.parser.HiddenTokenAwareTree;

public class CssClass extends ASTCssNode {

  private String name;

  public CssClass(HiddenTokenAwareTree token, String name) {
    super(token);
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public ASTCssNodeType getType() {
    return ASTCssNodeType.CSS_CLASS;
  }

  @Override
  public List<? extends ASTCssNode> getChilds() {
    return Collections.emptyList();
  }

}
