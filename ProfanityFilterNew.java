import java.io.*;
import java.util.*;

//https://stackoverflow.com/questions/18324054/filtering-bad-words-and-all-permutations-of-intentionally-misspelled-words
class TrieNode {
    // Structure of TrieNode
    Map<Character, TrieNode> children = new HashMap<Character, TrieNode>();
    TrieNode suffixLink;
    TrieNode outputLink;
    int pattern_ind; // denotes the end of the word
};

public class ProfanityFilterNew {
    private static String profanityresourceName;
    private static ClassLoader loader;
    private static InputStream profanityresourceStream;
    private static Properties profanityproperties;
    private static List<String> patterns;
    private static Set<String> wordList;
    private static TrieNode root;
    private static List<List<Integer>> indices;
    private static String searchString;

    public static void main(String[] args) throws IOException {
        searchString = "Youfuckasspussyfuckbastard";
        setProfanityProperties();

        /**********************************************
         * BRUTE FORCE
         **********************************************/

        long startTime1 = System.nanoTime();
        getIndicesBruteForce(searchString, patterns);
        long endTime1 = System.nanoTime();
        System.out.printf("\nBrute force algorithm ran in %.2f milliseconds.\n", (endTime1 - startTime1) / 1e6);

        /**********************************************
         * BRUTE FORCE
         **********************************************/

        /**********************************************
         * AHO CORASICK
         *********************************************/
        long startTime2 = System.nanoTime();
        setupAhoCorasick();

        searchPattern(root, searchString, indices);
        long endTime2 = System.nanoTime();
        System.out.printf("\nAho Corasick algorithm ran in %.2f milliseconds.\n", (endTime2 - startTime2) / 1e6);
        // BFS(root); //to check the structure of created automata
        getIndicesAhoCorasick(indices, patterns);
        long endTime3 = System.nanoTime();
        System.out.printf("\nPrinting results took %.2f milliseconds.\n", (endTime3 - endTime2) / 1e6);

        /**********************************************
         * AHO CORASICK
         *********************************************/

    }

    public static void setProfanityProperties() throws IOException {
        profanityresourceName = "util.properties";
        loader = Thread.currentThread().getContextClassLoader();
        profanityresourceStream = loader.getResourceAsStream(profanityresourceName);
        profanityproperties = new Properties();
        profanityproperties.load(profanityresourceStream);
        String words = getProfanityproperties("profanity.list", "");
        patterns = Arrays.asList(words.split(","));
        wordList = new HashSet<>(patterns);
    }

    public static String getProfanityproperties(String key, String defaultValue) {
        return profanityproperties.getProperty(key, defaultValue);
    }

    public static void getIndicesBruteForce(String searchString, List<String> patterns) {

        for (String pattern : patterns) {
            System.out.print("\nStarting Indices of \"" + pattern + "\": ");
            int index = searchString.indexOf(pattern);
            int count = 0;
            while (index >= 0) {
                System.out.print(index + ", ");
                index = searchString.indexOf(pattern, index + 1);
                count++;
            }
            if (count > 0)
                System.out.print("\nTotal occurrences of \"" + pattern + "\": " + count);
        }
    }

    public static void getIndicesAhoCorasick(List<List<Integer>> indices, List<String> patterns) {
        for (int i = 0; i < patterns.size(); i++) {
            System.out.print("Total occurrences of \"" + patterns.get(i) + "\": " + indices.get(i).size());
            if (indices.get(i).size() != 0) {
                System.out.print("\nStarting Indices: ");
                for (int j : indices.get(i))
                    System.out.print(j - patterns.get(i).length() + 1 + " ");
            }
            System.out.print("\n");
        }
    }

    public static TrieNode addNode() {
        // To add new trie node
        TrieNode temp = new TrieNode(); // allocating memory for new trie node
        // Assigning default values
        temp.suffixLink = null;
        temp.outputLink = null;
        temp.pattern_ind = -1;
        return temp;
    }

    public static void setupAhoCorasick() {
        root = addNode(); // allocating memory for root node
        buildTrie(root, patterns); // building trie out of patterns
        buildSuffixAndOutputLinks(root); // creating appropriate suffix and output links
        // vector<vector<int>> indices(k, vector<int>());
        indices = new ArrayList<List<Integer>>(patterns.size());
        for (int i = 0; i < patterns.size(); i++) {
            indices.add(new ArrayList<Integer>());
        }
    }

    public static void buildTrie(TrieNode root, List<String> patterns) {
        int x = 0;
        for (String pattern : patterns) {
            // Iterating over patterns
            TrieNode current = root;
            for (char ch : pattern.toCharArray()) {
                // iterating over characters in pattern[i]
                if (current.children.containsKey(ch)) // if node corresponding to current character is already present,
                                                      // follow it
                    current = current.children.get(ch);
                else {
                    TrieNode newChild = addNode(); // if node is not present, add new node to trie
                    current.children.put(ch, newChild);
                    current = newChild;
                }
            }
            current.pattern_ind = x++; // marking node as x-th pattern ends here
        }
    }

    public static void buildSuffixAndOutputLinks(TrieNode root) {
        root.suffixLink = root; // pointing suffix link of root back to itself
        Queue<TrieNode> queue = new LinkedList<TrieNode>(); // taking queue for breadth first search
        for (Map.Entry<Character, TrieNode> it : root.children.entrySet()) {
            queue.add(it.getValue()); // pushing nodes directly attached to root
            it.getValue().suffixLink = root; // setting suffix link of these nodes back to the root
        }
        while (!queue.isEmpty()) {
            TrieNode cur_state = queue.peek();
            queue.poll();
            for (Map.Entry<Character, TrieNode> it : cur_state.children.entrySet()) {
                // iterating over all child of current node
                char c = it.getKey();
                TrieNode temp = cur_state.suffixLink;
                while (!temp.children.containsKey(c) && temp != root) // finding longest proper suffix
                    temp = temp.suffixLink;
                if (temp.children.containsKey(c))
                    it.getValue().suffixLink = temp.children.get(c); // if proper suffix is found
                else
                    it.getValue().suffixLink = root; // if proper suffix not found
                queue.add(it.getValue());
            }
            // setting up output link
            if (cur_state.suffixLink.pattern_ind >= 0)
                cur_state.outputLink = cur_state.suffixLink;
            else
                cur_state.outputLink = cur_state.suffixLink.outputLink;
        }
    }

    public static void searchPattern(TrieNode root, String text, List<List<Integer>> indices) {
        TrieNode parent = root;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (parent.children.containsKey(c)) {
                // if link to character exists follow it
                parent = parent.children.get(c);
                if (parent.pattern_ind >= 0) {
                    indices.get(parent.pattern_ind).add(i); // if this node is marked then a pattern ends here
                }
                TrieNode temp = parent.outputLink;
                while (temp != null) {
                    indices.get(temp.pattern_ind).add(i); // follow all output links to get patterns ending at this
                                                          // position
                    temp = temp.outputLink;
                }
            } else {
                while (parent != root && !parent.children.containsKey(c)) // follow suffix links till matching suffix or
                                                                          // root is found
                    parent = parent.suffixLink;
                if (parent.children.containsKey(c))
                    i--;
            }
        }
    }

    public String convertWord(String userWords) {
        if (userWords == null || userWords.length() == 0)
            return new String("");
        Map<Character, Character> ruleEngine = new HashMap<Character, Character>();
        ruleEngine.put('4', 'a');
        ruleEngine.put('@', 'a');
        ruleEngine.put('Å', 'a');
        ruleEngine.put('å', 'a');
        ruleEngine.put('6', 'b');
        ruleEngine.put('8', 'b');
        ruleEngine.put('(', 'c');
        ruleEngine.put('3', 'e');
        ruleEngine.put('+', 'f');
        ruleEngine.put('#', 'h');
        ruleEngine.put('!', 'i');
        ruleEngine.put('|', 'i');
        ruleEngine.put('1', 'i');
        ruleEngine.put('0', 'o');
        ruleEngine.put('9', 'q');
        ruleEngine.put('$', 's');
        ruleEngine.put('&', 's');
        ruleEngine.put('5', 's');
        ruleEngine.put('7', 't');
        ruleEngine.put('*', 'x');
        ruleEngine.put('2', 'z');
        ruleEngine.put('A', 'a');
        ruleEngine.put('B', 'b');
        ruleEngine.put('C', 'c');
        ruleEngine.put('D', 'd');
        ruleEngine.put('E', 'e');
        ruleEngine.put('F', 'f');
        ruleEngine.put('G', 'g');
        ruleEngine.put('H', 'h');
        ruleEngine.put('I', 'i');
        ruleEngine.put('J', 'j');
        ruleEngine.put('K', 'k');
        ruleEngine.put('L', 'l');
        ruleEngine.put('M', 'm');
        ruleEngine.put('N', 'n');
        ruleEngine.put('O', 'o');
        ruleEngine.put('P', 'p');
        ruleEngine.put('Q', 'q');
        ruleEngine.put('R', 'r');
        ruleEngine.put('S', 's');
        ruleEngine.put('T', 't');
        ruleEngine.put('U', 'u');
        ruleEngine.put('V', 'v');
        ruleEngine.put('W', 'w');
        ruleEngine.put('X', 'x');
        ruleEngine.put('Y', 'y');
        ruleEngine.put('Z', 'z');
        char chars[] = userWords.toCharArray();
        for (int i = 0; i < userWords.length(); i++) {
            char currentChar = userWords.charAt(i);
            if (ruleEngine.containsKey(currentChar)) {
                chars[i] = ruleEngine.get(currentChar);
            }
        }
        String profaneWord = new String(chars);
        return profaneWord;
    }

}
