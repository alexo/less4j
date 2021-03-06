package org.porting.less4j.core.ast;

import java.util.List;

import org.porting.less4j.core.parser.HiddenTokenAwareTree;
import org.porting.less4j.utils.ArraysUtils;

public class SelectorAttribute extends ASTCssNode {

  private String name;
  private SelectorOperator operator;
  private String value;

  public SelectorAttribute(HiddenTokenAwareTree token, String name) {
    this(token, name, new SelectorOperator(new HiddenTokenAwareTree(), SelectorOperator.Operator.NONE), null);
  }
  
  public SelectorAttribute(HiddenTokenAwareTree token, String name, SelectorOperator operator, String value) {
    super(token);
    this.name = name;
    this.operator = operator;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public SelectorOperator getOperator() {
    return operator;
  }

  public void setOperator(SelectorOperator operator) {
    this.operator = operator;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public ASTCssNodeType getType() {
    return ASTCssNodeType.SELECTOR_ATTRIBUTE;
  }

  @Override
  public List<? extends ASTCssNode> getChilds() {
    return ArraysUtils.asNonNullList(operator);
  }

}
