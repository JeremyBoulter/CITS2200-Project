import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Collections;
import java.util.Arrays;
import java.util.Stack;

public class MyCITS2200Project implements CITS2200Project {

    private ArrayList<ArrayList<Integer>> WikipediaGraph; // the adjacency list
    private ArrayList<ArrayList<Integer>> WikipediaGraphTranspose; // a transposed version of the adjacency list
    private HashMap<String, Integer> urlToID; // map from url to id
    private ArrayList<String> idToUrl; // list of urls indexed by id

    //constructor
    public MyCITS2200Project() {
        WikipediaGraph = new ArrayList<>();
        WikipediaGraphTranspose = new ArrayList<>();
        urlToID = new HashMap<>();
        idToUrl = new ArrayList<>();
    }


    /**
     * Adds a vertex to the Wikipedia page graph, if the page does not already exist
     *
     * @param url the URL of the page to be added.
     */
    public void addVert(String url) {
        int id = urlToID.getOrDefault(url, -1);
        if (id == -1) {
            // the vertex does not exist, so create a new one
            id = WikipediaGraph.size(); // assign a new id
            urlToID.put(url, id); // add to map
            idToUrl.add(url); // add to list
            WikipediaGraph.add(new ArrayList<>()); // add a new list for neighbours
            WikipediaGraphTranspose.add(new ArrayList<>()); // adds a new list for reversed neighbours (SSC)
        }
    }

    /**
     * Adds an edge to the Wikipedia page graph.
     *
     * @param urlFrom the URL which has a link to urlTo.
     * @param urlTo   the URL which urlFrom has a link to.
     */
    public void addEdge(String urlFrom, String urlTo) {
        // ensure vertices exist in the graph
        addVert(urlFrom);
        addVert(urlTo);

        // get ids for the vertices
        int idFrom = urlToID.get(urlFrom);
        int idTo = urlToID.get(urlTo);

        // add the edge to the adjacency list
        WikipediaGraph.get(idFrom).add(idTo);
        // add the reverse edge to the transpose graph
        WikipediaGraphTranspose.get(idTo).add(idFrom);
    }

    /**
     * Finds the shortest path between two vertices in the graph represented by URLs.
     * The path is the minimum number of edges that must be traversed from the starting
     * vertex to the destination vertex.
     *
     * @param urlFrom the URL that represents the starting vertex.
     * @param urlTo the URL that represents the destination vertex.
     * @return the shortest path from urlFrom to urlTo as an integer. If either vertex
     *         does not exist or if no path exists between them, it returns -1.
     */
    public int getShortestPath(String urlFrom, String urlTo) {
        // Get the IDs of the source and target URLs
        int from = urlToID.getOrDefault(urlFrom, -1);
        int to = urlToID.getOrDefault(urlTo, -1);

        // Return -1 if either of the URLs is not in the graph
        if (from == -1 || to == -1) {
            return -1;
        }

        // Perform BFS on the graph starting from the source vertex
        int[] dist = BFS(from);

        // Return the distance of the target vertex from the source vertex
        // If no path exists, the value will be -1
        return dist[to];
    }


    /**
     * Computes the centers of the graph. A center is defined as a vertex with minimum eccentricity.
     * Eccentricity of a vertex is the maximum shortest path distance from this vertex to any other vertex.
     * If there are multiple centers, all of them are returned. If the graph is disconnected, nonexistent paths
     * are ignored when computing the eccentricity.
     *
     * @return an array of URLs representing the centers of the graph.
     */
    public String[] getCenters() {
        ArrayList<String> centers = new ArrayList<>();

        int minEccentricity = Integer.MAX_VALUE;

        for (int i = 0; i < idToUrl.size(); i++) {
            // Perform BFS from the current vertex
            int[] distances = BFS(i);

            // Compute the eccentricity of the current vertex (the maximum distance to any other vertex)
            int eccentricity = Arrays.stream(distances).filter(d -> d != -1).max().getAsInt();

            // Update the centers and the minimum eccentricity if necessary
            if (eccentricity < minEccentricity) {
                centers.clear();
                centers.add(idToUrl.get(i));
                minEccentricity = eccentricity;
            } else if (eccentricity == minEccentricity) {
                centers.add(idToUrl.get(i));
            }
        }
        return centers.toArray(new String[0]);
    }

     /**
     * Computes the strongly connected components of the graph.
     * A strongly connected component is a subgraph where there is a directed path between every pair of vertices.
     * @return a 2D array of strings representing the strongly connected components.
     */
    public String[][] getStronglyConnectedComponents() {
        Stack<Integer> stack = new Stack<>();

        // Step 1: Fill vertices in stack according to their finishing times
        boolean[] visited = new boolean[WikipediaGraph.size()];
        for (int i = 0; i < WikipediaGraph.size(); i++) {
            if (!visited[i])
                fillOrder(i, visited, stack);
        }

        // Step 2: Create the transposed graph (already done in constructor)

        // Step 2 and 3: Process all vertices in order defined by Stack
        visited = new boolean[WikipediaGraph.size()];
        ArrayList<ArrayList<String>> components = new ArrayList<>();
        while (!stack.isEmpty()) {
            // Pop a vertex from stack
            int v = stack.pop();

            // Print Strongly connected component of the popped vertex
            if (!visited[v]) {
                ArrayList<String> component = new ArrayList<>();
                DFSUtil(v, visited, component);
                components.add(component);
            }
        }

        // Step 5: Return the strongly connected components
        String[][] result = new String[components.size()][];
        for (int i = 0; i < components.size(); i++) {
            ArrayList<String> component = components.get(i);
            result[i] = component.toArray(new String[component.size()]);
        }
        return result;
    }

    private void fillOrder(int v, boolean visited[], Stack<Integer> stack) {
        // Mark the current node as visited
        visited[v] = true;

        // Recur for all vertices adjacent to this vertex
        for (int n : WikipediaGraph.get(v)) {
            if (!visited[n])
                fillOrder(n, visited, stack);
        }

        // All vertices reachable from v are processed by now, push v to Stack
        stack.push(v);
    }



    /**
     * Returns a Hamiltonian Path of the graph, if one exists.
     *
     * @return an array containing a Hamiltonian Path of the graph, or an empty array if no such path exists.
     */
    public String[] getHamiltonianPath() {
        int n = WikipediaGraph.size();
        int[][] dp = new int[n][1 << n];
        int[][] next = new int[n][1 << n];
        // Initialize dp and next arrays
        for (int[] row : dp)
            Arrays.fill(row, Integer.MAX_VALUE / 2);
        for (int[] row : next)
            Arrays.fill(row, -1);

        // dp[i][j] will be the shortest path that visits every node in the subset j exactly once, and ends at i
        for (int i = 0; i < n; i++)
            dp[i][1 << i] = 0;

        // Iterate over all subsets in increasing order of size
        for (int j = 1; j < (1 << n); j++) {
            for (int i = 0; i < n; i++) {
                if ((j & (1 << i)) != 0) { // if node i is in subset j
                    for (int k = 0; k < n; k++) {
                        if ((j & (1 << k)) != 0 && i != k) { // if node k is in subset j and k != i
                            if (dp[k][j ^ (1 << i)] + (WikipediaGraph.get(k).contains(i) ? 0 : Integer.MAX_VALUE / 2) < dp[i][j]) {
                                dp[i][j] = dp[k][j ^ (1 << i)] + (WikipediaGraph.get(k).contains(i) ? 0 : Integer.MAX_VALUE / 2);
                                next[i][j] = k;
                            }
                        }
                    }
                }
            }
        }

        // Find a node that can be the end of a Hamiltonian Path
        int idx = -1, minDist = Integer.MAX_VALUE / 2;
        for (int i = 0; i < n; i++) {
            if (dp[i][(1 << n) - 1] < minDist) {
                minDist = dp[i][(1 << n) - 1];
                idx = i;
            }
        }

        // Reconstruct the Hamiltonian Path
        if (idx != -1) {
            int curState = (1 << n) - 1;
            Stack<Integer> path = new Stack<>();
            while (idx != -1) {
                path.push(idx);
                int temp = idx;
                idx = next[idx][curState];
                curState = curState ^ (1 << temp);
            }
            String[] hamiltonianPath = new String[path.size()];
            for (int i = 0; i < hamiltonianPath.length; i++) {
                hamiltonianPath[i] = idToUrl.get(path.pop());
            }
            return hamiltonianPath;
        } else {
            return new String[0];
        }
    }


    /**
     * Performs a Breadth-First Search (BFS) on the Wikipedia graph from a given starting vertex.
     *
     * @param start The ID of the starting vertex.
     * @return An array of the shortest distances from the start vertex to every other vertex in the graph.
     *         If a vertex is unreachable from the start vertex, its corresponding entry in the array will be -1.
     */
    private int[] BFS(int start) {
        // Create an array to store the distance of each vertex from the start vertex
        int[] distances = new int[WikipediaGraph.size()];

        // Initialize all distances to -1, representing that the vertex is not yet reachable
        Arrays.fill(distances, -1);

        // The distance from the start vertex to itself is 0
        distances[start] = 0;

        // Create a queue for BFS and add the start vertex to it
        Queue<Integer> queue = new LinkedList<>();
        queue.offer(start);

        // While there are still vertices to process in the queue
        while (!queue.isEmpty()) {
            // Remove the next vertex from the queue
            int current = queue.poll();

            // For each neighbor of the current vertex
            for (int neighbor : WikipediaGraph.get(current)) {
                // If the neighbor has not been visited yet
                if (distances[neighbor] == -1) {
                    // Set its distance to one more than the current vertex's distance
                    distances[neighbor] = distances[current] + 1;

                    // Add the neighbor to the queue to explore its neighbors in a later iteration
                    queue.offer(neighbor);
                }
            }
        }

        // Return the computed distances
        return distances;
    }

    private void DFSUtil(int v, boolean visited[], ArrayList<String> component) {
        // Mark the current node as visited and print it
        visited[v] = true;
        component.add(idToUrl.get(v));

        // Recur for all vertices adjacent to this vertex
        for (int n : WikipediaGraphTranspose.get(v)) {
            if (!visited[n])
                DFSUtil(n, visited, component);
        }
    }


}
