/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.platon.pulsar.common;

import java.util.Collection;
import java.util.Iterator;

/**
 * A class for efficiently matching <code>String</code>s against a set of
 * suffixes. Zero-length <code>Strings</code> are ignored.
 *
 * @author vincent
 * @version $Id: $Id
 */
public class SuffixStringMatcher extends TrieStringMatcher {

    /**
     * Creates a new <code>PrefixStringMatcher</code> which will match
     * <code>String</code>s with any suffix in the supplied array.
     *
     * @param suffixes an array of {@link java.lang.String} objects.
     */
    public SuffixStringMatcher(String[] suffixes) {
        super();
        for (int i = 0; i < suffixes.length; i++)
            addPatternBackward(suffixes[i]);
    }

    /**
     * Creates a new <code>PrefixStringMatcher</code> which will match
     * <code>String</code>s with any suffix in the supplied
     * <code>Collection</code>
     *
     * @param suffixes a {@link java.util.Collection} object.
     */
    public SuffixStringMatcher(Collection<String> suffixes) {
        super();
        Iterator<String> iter = suffixes.iterator();
        while (iter.hasNext())
            addPatternBackward(iter.next());
    }

    /**
     * <p>main.</p>
     *
     * @param argv an array of {@link java.lang.String} objects.
     */
    public static final void main(String[] argv) {
        SuffixStringMatcher matcher = new SuffixStringMatcher(new String[]{"a",
                "abcd", "bcd", "bcdefg", "defg", "aac", "baz", "foo", "foobar"});

        String[] tests = {"a", "ac", "abcd", "abcdefg", "apple", "aa", "aac",
                "aaccca", "abaz", "baz", "bazooka", "fo", "foobar", "kite",};

        for (int i = 0; i < tests.length; i++) {
            System.out.println("testing: " + tests[i]);
            System.out.println("   matches: " + matcher.matches(tests[i]));
            System.out.println("  shortest: " + matcher.shortestMatch(tests[i]));
            System.out.println("   longest: " + matcher.longestMatch(tests[i]));
        }
    }

    /**
     * {@inheritDoc}
     *
     * Returns true if the given <code>String</code> is matched by a suffix in the
     * trie
     */
    public boolean matches(String input) {
        TrieNode node = root;
        for (int i = input.length() - 1; i >= 0; i--) {
            node = node.getChild(input.charAt(i));
            if (node == null)
                return false;
            if (node.isTerminal())
                return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * Returns the shortest suffix of <code>input</code> that is matched,
     * or <code>null</code> if no match exists.
     */
    public String shortestMatch(String input) {
        TrieNode node = root;
        for (int i = input.length() - 1; i >= 0; i--) {
            node = node.getChild(input.charAt(i));
            if (node == null)
                return null;
            if (node.isTerminal())
                return input.substring(i);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * Returns the longest suffix of <code>input</code> that is matched,
     * or <code>null</code> if no match exists.
     */
    public String longestMatch(String input) {
        TrieNode node = root;
        String result = null;
        for (int i = input.length() - 1; i >= 0; i--) {
            node = node.getChild(input.charAt(i));
            if (node == null)
                break;
            if (node.isTerminal())
                result = input.substring(i);
        }
        return result;
    }
}
