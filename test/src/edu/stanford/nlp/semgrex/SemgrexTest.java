package edu.stanford.nlp.semgrex;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import java.io.StringReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.stats.IntCounter;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.semgrex.*;

/**
 * @author John Bauer
 */
public class SemgrexTest extends TestCase {
  public void testMatchAll() {
    SemanticGraph graph = 
      SemanticGraph.valueOf("[ate subj:Bill dobj:[muffins nn:blueberry]]");
    Set<IndexedWord> words = graph.vertexSet();

    SemgrexPattern pattern = SemgrexPattern.compile("{}");
    SemgrexMatcher matcher = pattern.matcher(graph);
    String[] expectedMatches = {"ate", "Bill", "muffins", "blueberry"};
    for (int i = 0; i < expectedMatches.length; ++i) {
      assertTrue(matcher.findNextMatchingNode());
    }
    assertFalse(matcher.findNextMatchingNode());
  }

  public void testTest() {
    runTest("{}", "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "ate", "Bill", "muffins", "blueberry");

    try {
      runTest("{}", "[ate subj:Bill dobj:[muffins nn:blueberry]]",
              "ate", "Bill", "muffins", "foo");
      throw new RuntimeException();
    } catch (AssertionFailedError e) {
      // yay
    }

    try {
      runTest("{}", "[ate subj:Bill dobj:[muffins nn:blueberry]]",
              "ate", "Bill", "muffins");
      throw new RuntimeException();
    } catch (AssertionFailedError e) {
      // yay
    }

    try {
      runTest("{}", "[ate subj:Bill dobj:[muffins nn:blueberry]]",
              "ate", "Bill", "muffins", "blueberry", "blueberry");
      throw new RuntimeException();
    } catch (AssertionFailedError e) {
      // yay
    }
  }

  /**
   * This also tests negated node matches
   */
  public void testWordMatch() {
    runTest("{word:Bill}", "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "Bill");
    runTest("!{word:Bill}", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "ate", "muffins", "blueberry");
    runTest("!{word:Fred}", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "ate", "Bill", "muffins", "blueberry");
    runTest("!{word:ate}", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "Bill", "muffins", "blueberry");
    runTest("{word:/^(?!Bill).*$/}", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "ate", "muffins", "blueberry");
    runTest("{word:/^(?!Fred).*$/}", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "ate", "Bill", "muffins", "blueberry");
    runTest("{word:/^(?!ate).*$/}", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "Bill", "muffins", "blueberry");
    runTest("{word:muffins} >nn {word:blueberry}", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "muffins");
    runTest("{} << {word:ate}=a", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "Bill", "muffins", "blueberry");
    runTest("{} << !{word:ate}=a", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "blueberry");
    // blueberry should match twice because it has two ancestors
    runTest("{} << {}=a", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "Bill", "muffins", "blueberry", "blueberry"); 
  }

  public void testSimpleDependency() {
    // blueberry has two ancestors
    runTest("{} << {}", "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "Bill", "muffins", "blueberry", "blueberry");
    // ate has three descendants
    runTest("{} >> {}", "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "ate", "ate", "ate", "muffins");
    runTest("{} < {}", "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "Bill", "muffins", "blueberry");
    runTest("{} > {}", "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "ate", "ate", "muffins");
  }
 
  public void testNamedDependency() {
    runTest("{} << {word:ate}", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "Bill", "muffins", "blueberry");
    runTest("{} >> {word:blueberry}", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "ate", "muffins");
    runTest("{} >> {word:Bill}", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "ate");
    runTest("{} < {word:ate}", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "Bill", "muffins");
    runTest("{} > {word:blueberry}", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "muffins");
    runTest("{} > {word:muffins}", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "ate");
  }

  public void testNamedGovernor() {
    runTest("{word:blueberry} << {}", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "blueberry");
    runTest("{word:ate} << {}", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]");
    runTest("{word:blueberry} >> {}", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]");
    runTest("{word:muffins} >> {}", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "muffins");
    runTest("{word:Bill} >> {}", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]");
    runTest("{word:muffins} < {}", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "muffins");
    runTest("{word:muffins} > {}", 
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "muffins");
  }

  public void testTwoDependencies() {
    runTest("{} >> ({} >> {})",
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "ate");
    runTest("{} >> {word:Bill} >> {word:muffins}",
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "ate");
    runTest("{}=a >> {}=b >> {word:muffins}=c",
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "ate", "ate", "ate");
    runTest("{}=a >> {word:Bill}=b >> {}=c",
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "ate", "ate", "ate");
    runTest("{}=a >> {}=b >> {}=c",
            "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "ate", "ate", "ate", "ate", "ate",
            "ate", "ate", "ate", "ate", "muffins");
  }

  public void testRegex() {
    runTest("{word:/Bill/}", "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "Bill");

    runTest("{word:/ill/}", "[ate subj:Bill dobj:[muffins nn:blueberry]]");

    runTest("{word:/.*ill/}", "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "Bill");

    runTest("{word:/.*il/}", "[ate subj:Bill dobj:[muffins nn:blueberry]]");

    runTest("{word:/.*il.*/}", "[ate subj:Bill dobj:[muffins nn:blueberry]]",
            "Bill");
  }

  public void testReferencedRegex() {
    runTest("{word:/Bill/}", "[ate subj:Bill dobj:[bill det:the]]",
            "Bill");

    runTest("{word:/.*ill/}", "[ate subj:Bill dobj:[bill det:the]]",
            "Bill", "bill");

    runTest("{word:/[Bb]ill/}", "[ate subj:Bill dobj:[bill det:the]]",
            "Bill", "bill");

    // TODO: implement referencing regexes
  }

  static public SemanticGraph makeComplicatedGraph() {
    SemanticGraph graph = new SemanticGraph();
    String[] words = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
    IndexedWord[] nodes = new IndexedWord[words.length];
    for (int i = 0; i < words.length; ++i) {
      IndexedWord word = new IndexedWord("test", 1, i + 1);
      word.setWord(words[i]);
      word.setValue(words[i]);
      nodes[i] = word;
      graph.addVertex(word);
    }
    graph.setRoot(nodes[0]);
    // this graph isn't supposed to make sense
    graph.addEdge(nodes[0], nodes[1], 
                  EnglishGrammaticalRelations.MODIFIER, 1.0);
    graph.addEdge(nodes[0], nodes[2],
                  EnglishGrammaticalRelations.DIRECT_OBJECT, 1.0);
    graph.addEdge(nodes[0], nodes[3],
                  EnglishGrammaticalRelations.INDIRECT_OBJECT, 1.0);
    graph.addEdge(nodes[1], nodes[4],
                  EnglishGrammaticalRelations.MARKER, 1.0);
    graph.addEdge(nodes[2], nodes[4],
                  EnglishGrammaticalRelations.EXPLETIVE, 1.0);
    graph.addEdge(nodes[3], nodes[4],
                  EnglishGrammaticalRelations.ADJECTIVAL_COMPLEMENT, 1.0);
    graph.addEdge(nodes[4], nodes[5],
                  EnglishGrammaticalRelations.ADJECTIVAL_MODIFIER, 1.0);
    graph.addEdge(nodes[4], nodes[6],
                  EnglishGrammaticalRelations.ADVERBIAL_MODIFIER, 1.0);
    graph.addEdge(nodes[4], nodes[8],
                  EnglishGrammaticalRelations.MODIFIER, 1.0);
    graph.addEdge(nodes[5], nodes[7],
                  EnglishGrammaticalRelations.POSSESSION_MODIFIER, 1.0);
    graph.addEdge(nodes[6], nodes[7],
                  EnglishGrammaticalRelations.POSSESSIVE_MODIFIER, 1.0);
    graph.addEdge(nodes[7], nodes[8],
                  EnglishGrammaticalRelations.AGENT, 1.0);
    graph.addEdge(nodes[8], nodes[9],
                  EnglishGrammaticalRelations.DETERMINER, 1.0);

    return graph;
  }

  /**
   * Test that governors, dependents, ancestors, descendants are all
   * returned with multiplicity 1 if there are multiple paths to the
   * same node.
   */
  public void testComplicatedGraph() {
    SemanticGraph graph = makeComplicatedGraph();

    runTest("{} < {word:A}", graph,
            "B", "C", "D");

    runTest("{} > {word:E}", graph,
            "B", "C", "D");

    runTest("{} > {word:J}", graph,
            "I");

    runTest("{} < {word:E}", graph,
            "F", "G", "I");

    runTest("{} < {word:I}", graph,
            "J");

    runTest("{} << {word:A}", graph,
            "B", "C", "D", "E", "F", "G", "H", "I", "J");

    runTest("{} << {word:B}", graph,
            "E", "F", "G", "H", "I", "J");

    runTest("{} << {word:C}", graph,
            "E", "F", "G", "H", "I", "J");

    runTest("{} << {word:D}", graph,
            "E", "F", "G", "H", "I", "J");

    runTest("{} << {word:E}", graph,
            "F", "G", "H", "I", "J");

    runTest("{} << {word:F}", graph,
            "H", "I", "J");

    runTest("{} << {word:G}", graph,
            "H", "I", "J");

    runTest("{} << {word:H}", graph,
            "I", "J");

    runTest("{} << {word:I}", graph,
            "J");

    runTest("{} << {word:J}", graph);

    runTest("{} << {word:K}", graph);

    runTest("{} >> {word:A}", graph);

    runTest("{} >> {word:B}", graph, "A");

    runTest("{} >> {word:C}", graph, "A");

    runTest("{} >> {word:D}", graph, "A");

    runTest("{} >> {word:E}", graph,
            "A", "B", "C", "D");

    runTest("{} >> {word:F}", graph,
            "A", "B", "C", "D", "E");

    runTest("{} >> {word:G}", graph,
            "A", "B", "C", "D", "E");

    runTest("{} >> {word:H}", graph,
            "A", "B", "C", "D", "E", "F", "G");

    runTest("{} >> {word:I}", graph,
            "A", "B", "C", "D", "E", "F", "G", "H");

    runTest("{} >> {word:J}", graph,
            "A", "B", "C", "D", "E", "F", "G", "H", "I");

    runTest("{} >> {word:K}", graph);
  }

  public void testRelationType() {
    SemanticGraph graph = makeComplicatedGraph();
    runTest("{} <<mod {}", graph,
            "B", "E", "F", "G", "H", "I", "I", "J", "J");

    runTest("{} >>det {}", graph,
            "A", "B", "C", "D", "E", "F", "G", "H", "I");

    runTest("{} >>det {word:J}", graph,
            "A", "B", "C", "D", "E", "F", "G", "H", "I");
  }

  public void testExactDepthRelations() {
    SemanticGraph graph = makeComplicatedGraph();
    runTest("{} 2,3<< {word:A}", graph, "E", "F", "G", "I");

    runTest("{} 2,2<< {word:A}", graph, "E");

    runTest("{} 1,2<< {word:A}", graph, "B", "C", "D", "E");

    runTest("{} 0,2<< {word:A}", graph, "B", "C", "D", "E");

    runTest("{} 0,10<< {word:A}", graph, 
            "B", "C", "D", "E", "F", "G", "H", "I", "J");

    runTest("{} 0,10>> {word:J}", graph, 
            "A", "B", "C", "D", "E", "F", "G", "H", "I");

    runTest("{} 2,3>> {word:J}", graph, 
            "B", "C", "D", "E", "F", "G", "H");

    runTest("{} 2,2>> {word:J}", graph, 
            "E", "H");

    // use this method to avoid the toString() test, since we expect it
    // to use 2,2>> instead of 2>>
    runTest(SemgrexPattern.compile("{} 2>> {word:J}"), graph, 
            "E", "H");

    runTest("{} 1,2>> {word:J}", graph, 
            "E", "H", "I");
  }

  public void testNamedNode() {
    SemanticGraph graph = makeComplicatedGraph();
    
    runTest("{} >dobj ({} >expl {})", graph, "A");

    SemgrexPattern pattern = 
      SemgrexPattern.compile("{} >dobj ({} >expl {}=foo)");
    SemgrexMatcher matcher = pattern.matcher(graph);
    assertTrue(matcher.find());
    assertEquals(1, matcher.getNodeNames().size());
    assertEquals("E", matcher.getNode("foo").toString());
    assertEquals("A", matcher.getMatch().toString());
    assertFalse(matcher.find());

    pattern = SemgrexPattern.compile("{} >dobj ({} >expl {}=foo) >mod {}");
    matcher = pattern.matcher(graph);
    assertTrue(matcher.find());
    assertEquals(1, matcher.getNodeNames().size());
    assertEquals("E", matcher.getNode("foo").toString());
    assertEquals("A", matcher.getMatch().toString());
    assertFalse(matcher.find());

    pattern = 
      SemgrexPattern.compile("{} >dobj ({} >expl {}=foo) >mod ({} >mark {})");
    matcher = pattern.matcher(graph);
    assertTrue(matcher.find());
    assertEquals(1, matcher.getNodeNames().size());
    assertEquals("E", matcher.getNode("foo").toString());
    assertEquals("A", matcher.getMatch().toString());
    assertFalse(matcher.find());

    pattern = 
      SemgrexPattern.compile("{} >dobj ({} >expl {}=foo) >mod ({} > {})");
    matcher = pattern.matcher(graph);
    assertTrue(matcher.find());
    assertEquals(1, matcher.getNodeNames().size());
    assertEquals("E", matcher.getNode("foo").toString());
    assertEquals("A", matcher.getMatch().toString());
    assertFalse(matcher.find());

    pattern = 
      SemgrexPattern.compile("{} >dobj ({} >expl {}=foo) >mod ({} > {}=foo)");
    matcher = pattern.matcher(graph);
    assertTrue(matcher.find());
    assertEquals(1, matcher.getNodeNames().size());
    assertEquals("E", matcher.getNode("foo").toString());
    assertEquals("A", matcher.getMatch().toString());
    assertFalse(matcher.find());

    pattern = 
      SemgrexPattern.compile("{} >dobj ({} >expl {}=foo) >mod ({}=foo > {})");
    matcher = pattern.matcher(graph);
    assertFalse(matcher.find());
  }

  public void testPartition() {
    SemanticGraph graph = makeComplicatedGraph();

    runTest("{}=a >> {word:E}", graph, "A", "B", "C", "D");
    runTest("{}=a >> {word:E} : {}=a >> {word:B}", graph, "A");
  }

  public void testInitialConditions() {
    SemanticGraph graph = makeComplicatedGraph();

    SemgrexPattern pattern = 
      SemgrexPattern.compile("{}=a >> {}=b : {}=a >> {}=c");
    Map<String, IndexedWord> variables = new HashMap<String, IndexedWord>();
    variables.put("b", graph.getNodeByIndex(5));
    variables.put("c", graph.getNodeByIndex(2));
    SemgrexMatcher matcher = pattern.matcher(graph, variables);
    assertTrue(matcher.find());
    assertEquals(3, matcher.getNodeNames().size());
    assertEquals("A", matcher.getNode("a").toString());
    assertEquals("E", matcher.getNode("b").toString());
    assertEquals("B", matcher.getNode("c").toString());
    assertEquals("A", matcher.getMatch().toString());
    assertFalse(matcher.find());
  }

  static public void outputResults(String pattern, String graph, 
                                   String ... ignored) {
    outputResults(SemgrexPattern.compile(pattern), 
                  SemanticGraph.valueOf(graph));
  }
  
  static public void outputResults(String pattern, SemanticGraph graph, 
                                   String ... ignored) {
    outputResults(SemgrexPattern.compile(pattern), graph);
  }
  
  static public void outputResults(SemgrexPattern pattern, SemanticGraph graph,
                                   String ... ignored) {
    System.out.println("Matching pattern " + pattern + " to\n" + graph +
                       "  :" + (pattern.matcher(graph).matches() ? 
                                "matches" : "doesn't match"));
    System.out.println();
    pattern.prettyPrint();
    System.out.println();    
    SemgrexMatcher matcher = pattern.matcher(graph);
    while (matcher.find()) {
      System.out.println("  " + matcher.getMatch());
      Set<String> nodeNames = matcher.getNodeNames();
      if (nodeNames != null && nodeNames.size() > 0) {
        for (String name : nodeNames) {
          System.out.println("    " + name + ": " + matcher.getNode(name));
        }
      }
    }
  }

  public void comparePatternToString(String pattern) {
    SemgrexPattern semgrex = SemgrexPattern.compile(pattern);
    String tostring = semgrex.toString();
    tostring = tostring.replaceAll(" +", " ");
    assertEquals(pattern.trim(), tostring.trim());
  }
  
  public void runTest(String pattern, String graph, 
                      String ... expectedMatches) {
    comparePatternToString(pattern);
    runTest(SemgrexPattern.compile(pattern), SemanticGraph.valueOf(graph),
            expectedMatches);
  }

  public void runTest(String pattern, SemanticGraph graph, 
                      String ... expectedMatches) {
    comparePatternToString(pattern);
    runTest(SemgrexPattern.compile(pattern), graph, expectedMatches);
  }

  public void runTest(SemgrexPattern pattern, SemanticGraph graph,
                      String ... expectedMatches) {
    // results are not in the order I would expect.  Using a counter
    // allows them to be in any order
    IntCounter<String> counts = new IntCounter<String>();
    for (int i = 0; i < expectedMatches.length; ++i) {
      counts.incrementCount(expectedMatches[i]);
    }
    IntCounter<String> originalCounts = new IntCounter<String>(counts);

    SemgrexMatcher matcher = pattern.matcher(graph);

    for (int i = 0; i < expectedMatches.length; ++i) {
      if (!matcher.find()) {
        throw new AssertionFailedError("Expected " + expectedMatches.length +
                                       " matches for pattern " + pattern + 
                                       " on " + graph + ", only got " + i);
      }
      String match = matcher.getMatch().toString();
      if (!counts.containsKey(match)) {
        throw new AssertionFailedError("Unexpected match " + match + 
                                       " for pattern " + pattern + 
                                       " on " + graph);
      }
      counts.decrementCount(match);
      if (counts.getCount(match) < 0) {
        throw new AssertionFailedError("Found too many matches for " + match +
                                       " for pattern " + pattern + 
                                       " on " + graph);
      }
    }
    if (matcher.findNextMatchingNode()) {
      throw new AssertionFailedError("Found more than " + 
                                     expectedMatches.length + 
                                     " matches for pattern " + pattern + 
                                     " on " + graph + "... extra match is " +
                                     matcher.getMatch());
    }
  }
}
