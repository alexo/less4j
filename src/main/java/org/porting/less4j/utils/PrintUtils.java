package org.porting.less4j.utils;

import org.porting.less4j.core.parser.LessParser;

public class PrintUtils {
  public static String toName(int tokenType) {
    if (tokenType==-1)
      return "EOF";
    
    if (tokenType>=LessParser.tokenNames.length)
      return "Unknown: " + tokenType;
    
    return LessParser.tokenNames[tokenType];
  }


}
