/*
 * Copyright 2015 Steve Ash
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

package com.github.steveash.jg2p.seq;

import java.util.Iterator;
import java.util.List;

/**
 * @author Steve Ash
 */
public class TagResult {

  // predicted phones list; each entry is one phoneme, EPS stripped out
  private final List<String> phones;
  // contains a list of the graphone's phoneme's part so there can be EPS and 1+ phones
  private final List<String> graphonePhones;
  private final double logScore;

  public TagResult(List<String> graphonePhones, List<String> phones, double logScore) {
    this.graphonePhones = graphonePhones;
    this.phones = phones;
    this.logScore = logScore;
  }

  public double sequenceLogProbability() {
    return logScore;
  }

  public double sequenceProbability() {
    return Math.exp(logScore);
  }

  public List<String> phones() {
    return phones;
  }

  public List<String> phoneGrams() {
    return graphonePhones;
  }

  public boolean isEqualTo(Iterable<String> expected) {
    Iterator<String> iter = expected.iterator();
    for (int i = 0; i < phones.size(); i++) {
      if (!iter.hasNext()) {
        return false;
      }
      String next = iter.next();
      if (!phones.get(i).equals(next)) {
        return false;
      }
    }
    // got through all of the phones, make sure that the iterator is empty too
    return !iter.hasNext();
  }
}
