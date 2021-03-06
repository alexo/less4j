package org.porting.less4j.core.ast;

import org.porting.less4j.core.parser.HiddenTokenAwareTree;

public class IndirectVariable extends Variable {
  
  public IndirectVariable(HiddenTokenAwareTree underlyingStructure, String name) {
    super(underlyingStructure, name);
  }

  @Override
  public ASTCssNodeType getType() {
    return ASTCssNodeType.INDIRECT_VARIABLE;
  }

}
