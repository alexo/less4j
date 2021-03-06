package org.porting.less4j.debugutils;

import java.util.Map;

import org.porting.less4j.core.ast.NamedColorExpression;

/**
 * Transforms less.js output to equivalent css that should be generated by 
 * less4j. 
 * 
 * Consider this util class only. Transformation is NOT general enough to
 * translate any less.js output into equivalent less4j output. It is specific 
 * for encountered cases. 
 *
 */
public class LessjsToLess4jTransformer {

  public String transform(String css) {
    //less.js sometimes but not always translates color names into color hashes in some cases
    //less4j never translates them,
    Map<String, String> allNames = NamedColorExpression.getAllNames();
    for (String name : allNames.keySet()) {
      css = css.replace("background-color: " + allNames.get(name), "background-color: " + name);
      css = css.replace("color: " + allNames.get(name), "color: " + name);
      css = css.replace("background: " + allNames.get(name), "background: " + name);
      css = css.replace("border: thick solid " + allNames.get(name), "border: thick solid " + name);
      css = css.replace("border: thin "+allNames.get(name)+" solid", "border: thin "+name+" solid");
      
    }
    //whitespace around comments differences
    css = css.replace("}\n/*", "} /*");
    css = css.replace("} /* let's try some pseudos that", "}\n/* let's try some pseudos that");
    //other whitespace differences 
    css = css.replace(" ;", ";");
    css = css.replace("! important", "!important");
    return css;
  }

}
