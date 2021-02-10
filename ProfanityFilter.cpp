#include <iostream>
#include <vector>
#include <map>
#include <queue>
using namespace std;

struct trie_node
{ // structure of trie node
    map<char, trie_node *> child;
    trie_node *suffix_link;
    trie_node *output_link;
    int pattern_ind;
};

typedef struct trie_node node;

node *add_node()
{                          // to add new trie node
    node *temp = new node; // allocating memory for new trie node

    // assigning default values
    temp->suffix_link = nullptr;
    temp->output_link = nullptr;
    temp->pattern_ind = -1;

    return temp;
}

void build_Trie(node *root, vector<string> &patterns)
{
    for (int i = 0; i < patterns.size(); i++)
    { // iterating over patterns
        node *cur = root;
        for (auto c : patterns[i])
        {                            // iterating over characters in pattern[i]
            if (cur->child.count(c)) // if node corresponding to current character is already present, follow it
                cur = cur->child[c];
            else
            {
                node *new_child = add_node(); // if node is not present, add new node to trie
                cur->child.insert({c, new_child});
                cur = new_child;
            }
        }
        cur->pattern_ind = i; // marking node as i-th pattern ends here
    }
}

void build_suffix_and_output_links(node *root)
{
    root->suffix_link = root; // pointing suffix link of root back to itself

    queue<node *> qu; // taking queue for breadth first search

    for (auto &it : root->child)
    {
        qu.push(it.second);            // pushing nodes directly attached to root
        it.second->suffix_link = root; // setting suffix link of these nodes back to the root
    }

    while (qu.size())
    {
        node *cur_state = qu.front();
        qu.pop();

        for (auto &it : cur_state->child)
        { // iterating over all child of current node
            char c = it.first;
            node *temp = cur_state->suffix_link;

            while (temp->child.count(c) == 0 && temp != root) // finding longest proper suffix
                temp = temp->suffix_link;

            if (temp->child.count(c))
                it.second->suffix_link = temp->child[c]; // if proper suffix is found
            else
                it.second->suffix_link = root; // if proper suffix not found

            qu.push(it.second);
        }

        // setting up output link
        if (cur_state->suffix_link->pattern_ind >= 0)
            cur_state->output_link = cur_state->suffix_link;
        else
            cur_state->output_link = cur_state->suffix_link->output_link;
    }
}

void search_pattern(node *root, string &text, auto &indices)
{
    node *parent = root;

    for (int i = 0; i < text.length(); i++)
    {
        char c = text[i];
        if (parent->child.count(c))
        { // if link to character exists follow it
            parent = parent->child[c];

            if (parent->pattern_ind >= 0)
                indices[parent->pattern_ind].push_back(i); // if this node is marked then a pattern ends here

            node *temp = parent->output_link;
            while (temp != nullptr)
            {
                indices[temp->pattern_ind].push_back(i); // follow all output links to get patterns ending at this position
                temp = temp->output_link;
            }
        }
        else
        {
            while (parent != root && parent->child.count(c) == 0) // follow suffix links till matching suffix or root is found
                parent = parent->suffix_link;

            if (parent->child.count(c))
                i--;
        }
    }
}

void BFS(node *root)
{ // utility function to print the automata
    queue<pair<char, node *>> qu;
    cout << "\n"
         << root << "\n"
         << root->suffix_link << "\n"
         << root->output_link << "\n"
         << root->pattern_ind << "\n\n";

    for (auto &it : root->child)
        qu.push(it);

    while (qu.size())
    {
        auto temp = qu.front();
        qu.pop();

        cout << temp.second << "\n"
             << temp.first << "\n"
             << temp.second->suffix_link << "\n"
             << temp.second->output_link << "\n"
             << temp.second->pattern_ind << "\n\n";

        for (auto &it : temp.second->child)
            qu.push(it);
    }
}

int main()
{
    int k; //Number of patterns
    cout << "Enter number of patterns:";
    cin >> k;

    string text;                // given string in which pattern is to be searched
    vector<string> patterns(k); // vector or array of patterns

    for (int i = 0; i < k; i++)
    {
        cout << "Enter pattern[" << i << "]: ";
        cin >> patterns[i];
    }
    cout << "Enter Search String:";
    cin >> text;

    node *root = add_node(); // allocating memory for root node

    build_Trie(root, patterns); // building trie out of patterns

    build_suffix_and_output_links(root); // creating appropriate suffix and output links

    vector<vector<int>> indices(k, vector<int>());
    search_pattern(root, text, indices);

    // BFS(root); to check the structure of created automata

    for (int i = 0; i < k; i++)
    {
        cout << "Total occurrences of \"" << patterns[i] << "\": " << indices[i].size();

        if (indices[i].size())
            cout << "\nIndices: ";

        for (auto &j : indices[i])
            cout << j - patterns[i].length() + 1 << " ";
        cout << "\n\n";
    }

    return 0;
}