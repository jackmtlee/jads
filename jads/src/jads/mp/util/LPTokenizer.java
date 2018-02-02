package jads.mp.util;

import java.util.*;

public class LPTokenizer {

    public final String str;
    public final char[] separators;

    private int prevStart, prevEnd, start, end = -1;
    private boolean tokenIsNumber = false;

    public LPTokenizer(String str) {
        this.str = str != null ? str : "";
        this.separators = new char[]{ ' ', '\t', '\n' };
    }

    public LPTokenizer(String str, String separators) {
        this.str = str;
        this.separators = separators.toCharArray();
    }

    public boolean hasToken() {
        int start = end + 1;
        while (start < str.length() && isSeparator(str.charAt(start)))
            start++;
        if (start < str.length()) {
            end = start - 1;
            return true;
        }
        return false;
    }

    public void finish() {
        start = end = str.length();
    }

    public double nextDouble() {
        while (start < str.length()) {
            try {
                String token = nextToken().trim();
                return Double.parseDouble(token);
            }
            catch (NumberFormatException ignored) {}
        }
        throw new NoSuchElementException();
    }

    public int nextInt() {
        while (start < str.length()) {
            try {
                String token = nextToken().trim();
                return Integer.parseInt(token);
            }
            catch (NumberFormatException ignored) {}
        }
        throw new NoSuchElementException();
    }

    public String nextToken() {
        prevStart = start;
        prevEnd = end;
        skipToken();
        return str.substring(start, end);
    }

    public boolean isNumber() {
        return tokenIsNumber;
    }

    public void skipToken() {
        start = end + 1;
        while (start < str.length() && isSeparator(str.charAt(start))) start++;

        end = start + 1;
        if (start < str.length()) {
            if (isDigit(str.charAt(start))) {
                tokenIsNumber = true;
                while (end < str.length() && !isSeparator(str.charAt(end)) && isDigit(str.charAt(end)))
                    end++;
            }
            else {
                tokenIsNumber = false;
                while (end < str.length() && !isSeparator(str.charAt(end))) {
                    end++;
                }
            }
        }
    }

    public void undo() {
        start = prevStart;
        end = prevEnd;
    }

    private boolean isDigit(char c) {
        return Character.isDigit(c) || c == '+' || c == '-';
    }

    private boolean isSeparator(char c) {
        for (char separator : separators)
            if (c == separator)
                return true;
        return false;
    }
}
