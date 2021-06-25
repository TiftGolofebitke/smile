/*
 * Copyright (c) 2010-2021 Haifeng Li. All rights reserved.
 *
 * Smile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Smile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Smile.  If not, see <https://www.gnu.org/licenses/>.
 */

package smile.neighbor;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;
import smile.math.distance.HammingDistance;
import smile.hash.SimHash;
import smile.util.IntArrayList;

/**
 * Locality-Sensitive Hashing for Signatures.
 * LSH is an efficient algorithm for approximate nearest neighbor search
 * in high dimensional spaces by performing probabilistic dimension reduction of data.
 * The basic idea is to hash the input items so that similar items are mapped to the same
 * buckets with high probability (the number of buckets being much smaller
 * than the universe of possible input items).
 * To avoid computing the similarity of every pair of sets or their signatures.
 * If we are given signatures for the sets, we may divide them into bands, and only
 * measure the similarity of a pair of sets if they are identical in at least one band.
 * By choosing the size of bands appropriately, we can eliminate from
 * consideration most of the pairs that do not meet our threshold of similarity.
 * <p>
 * By default, the query object (reference equality) is excluded from the neighborhood.
 * Note that you may observe weird behavior with String objects. JVM will pool the string
 * literal objects. So the below variables
 * <code>
 *     String a = "ABC";
 *     String b = "ABC";
 *     String c = "AB" + "C";
 * </code>
 * are actually equal in reference test <code>a == b == c</code>. With toy data that you
 * type explicitly in the code, this will cause problems. Fortunately, the data would be
 * read from secondary storage in production.
 * </p>
 *
 * <h2>References</h2>
 * <ol>
 * <li>Moses S. Charikar. Similarity Estimation Techniques from Rounding Algorithms</li>
 * </ol>
 *
 * @see LSH
 * @see smile.hash.SimHash
 *
 * @author Qiyang Zuo
 */
public class SNLSH<K, V> implements RNNSearch<K, V>, Serializable {
    private static final long serialVersionUID = 2L;

    /** Hash function mask. */
    private final long mask;
    /** The number of bits of hash function. */
    private static final int BITS = 64;
    /**
     * Signature fractions
     */
    private final LinkedHashMap<Long, IntArrayList>[] bands;
    /**
     * The data objects.
     */
    private final List<V> data = new ArrayList<>();
    /**
     * The keys of data objects.
     */
    private final List<K> keys = new ArrayList<>();
    /**
     * The signatures generated by simhash
     */
    private final List<Long> signatures = new ArrayList<>();
    /**
     * SimHash function.
     */
    private final SimHash<K> simhash;

    /**
     * Constructor.
     * @param L the number of bands/hash tables.
     * @param hash simhash function.
     */
    @SuppressWarnings("unchecked")
    public SNLSH(int L, SimHash<K> hash) {
        if (L < 2 || L > 32) {
            throw new IllegalArgumentException("Invalid band size!");
        }

        simhash = hash;
        bands = (LinkedHashMap<Long, IntArrayList>[]) Array.newInstance(LinkedHashMap.class, L);
        for (int i = 0; i < L; i++) {
            bands[i] = new LinkedHashMap<>();
        }
        mask = -1 >>> (BITS / L * (L - 1));
    }

    /**
     * Adds a new item.
     * @param key the key.
     * @param value the value.
     */
    public void put(K key, V value) {
        int index = data.size();
        keys.add(key);
        data.add(value);

        long signature = simhash.hash(key);
        signatures.add(signature);

        for (int i = 0; i < bands.length; i++) {
            long bandKey = bandHash(signature, i);
            IntArrayList bucket = bands[i].get(bandKey);
            if (bucket == null) {
                bucket = new IntArrayList();
            }
            bucket.add(index);
            bands[i].put(bandKey, bucket);
        }
    }

    @Override
    public void range(K q, double radius, List<Neighbor<K, V>> neighbors) {
        if (radius <= 0 || radius != (int) radius) {
            throw new IllegalArgumentException("The parameter radius has to be an integer: " + radius);
        }

        long fpq = simhash.hash(q);
        Set<Integer> candidates = getCandidates(q);
        for (int index : candidates) {
            int distance = HammingDistance.d(fpq, signatures.get(index));
            if (distance <= radius) {
                neighbors.add(new Neighbor<>(keys.get(index), data.get(index), index, distance));
            }
        }
    }

    /**
     * Calculates the hash value for a band.
     */
    private long bandHash(long hash, int band) {
        return hash >>> ((band * (BITS / bands.length))) & mask;
    }

    /** Returns the nearest neighbor candidates. */
    private Set<Integer> getCandidates(K q) {
        Set<Integer> candidates = new LinkedHashSet<>();
        long sign = simhash.hash(q);
        for (int i = 0; i < bands.length; i++) {
            long bandKey = bandHash(sign, i);
            IntArrayList bucket = bands[i].get(bandKey);
            if (bucket != null) {
                for (int j = 0; j < bucket.size(); j++) {
                    candidates.add(bucket.get(j));
                }
            }
        }
        return candidates;
    }
}
