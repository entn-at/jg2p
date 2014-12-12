/*
 * Copyright 2014 Steve Ash
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.steveash.jg2p;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

import com.github.steveash.jg2p.align.GramOptions;
import com.github.steveash.jg2p.util.NestedLoopPairIterable;

import org.apache.commons.lang3.tuple.Pair;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;

/**
 * Utility class for iterating over grams, lists of words X Y into grams, etc.
 *
 * @author Steve Ash
 */
public class Grams {
  public static final String EPSILON = "";

  private Grams() { }

  public static Iterable<Pair<String, String>> wordPairsToAllGrams(Iterable<? extends Pair<Word, Word>> words,
                                                                             final GramOptions opts) {

    return concat(transform(words, new Function<Pair<Word, Word>, Iterable<Pair<String, String>>>() {
      @Override
      public Iterable<Pair<String, String>> apply(Pair<Word, Word> input) {
        return gramProduct(input.getLeft(), input.getRight(), opts);
      }
    }));
  }

  public static Iterable<Pair<String, String>> gramProduct(Word xWord, Word yWord, GramOptions opts) {

    Iterable<String> xGrams = xWord.gramsSizes(opts.getMinXGram(), opts.getMaxXGram());
    Iterable<String> yGrams = yWord.gramsSizes(opts.getMinYGram(), opts.getMaxYGram());
    NestedLoopPairIterable<String, String> pairs = NestedLoopPairIterable.of(xGrams, yGrams);

    Iterable<Pair<String, String>> xEps = ImmutableList.of();
    Iterable<Pair<String, String>> epsY = ImmutableList.of();

    if (opts.isIncludeXEpsilons()) {
      xEps = gramEpsilons(xGrams);
    }
    if (opts.isIncludeEpsilonYs()) {
      epsY = epsilonGrams(yGrams);
    }

    return concat(pairs, xEps, epsY);
  }

  public static Iterable<Pair<String, String>> gramEpsilons(Iterable<String> grams) {
    return transform(grams, toGramEps);
  }

  public static Iterable<Pair<String, String>> epsilonGrams(Iterable<String> grams) {
    return transform(grams, toEpsGram);
  }

  private static final Function<String, Pair<String, String>> toGramEps = new Function<String, Pair<String, String>>() {
    @Override
    public Pair<String, String> apply(String input) {
      return Pair.of(input, EPSILON);
    }
  };

  private static final Function<String, Pair<String, String>> toEpsGram = new Function<String, Pair<String, String>>() {
    @Override
    public Pair<String, String> apply(String input) {
      return Pair.of(EPSILON, input);
    }
  };
}