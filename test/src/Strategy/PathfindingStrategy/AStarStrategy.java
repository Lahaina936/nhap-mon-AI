/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Strategy.PathfindingStrategy;

import Model.Grid;
import Model.Tile;
import Strategy.HeuristicStrategy.HeuristicStrategy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 *
 * @author frank
 */
public class AStarStrategy extends PathfindingStrategy
{
    private final boolean findOptimalSolution;
    private final HeuristicStrategy heuristic;
    public static enum Heuristic
    {
        Pythagoras,
        Manhattan,
        Diagonal,
        Eudclidean
    }
    
    
    public AStarStrategy(boolean findOptimalSolution, HeuristicStrategy heuristic)
    {
        super();
        this.findOptimalSolution = findOptimalSolution;
        this.heuristic = heuristic;
    }

    @Override
    public int runPathfinder(Grid model, List<Tile> path)
    {
        List<Node> nodes = new ArrayList<>();
        Node rootNode = null;
        Node targetNode = null;
        
        // Initializes all nodes from the grid tiles
        for(Tile tile : model.getTiles())
        {
            Node node = new Node(tile);
            
            if(model.getRoot() == tile)
                rootNode = node;
            else if(model.getTarget() == tile)
                targetNode = node;
                
            nodes.add(node);
        }
        
        // Sets the neighbors of all nodes
        nodes.forEach((node) ->
        {
            node.setNeighbors(nodes, model);
        });
        
        executeAStar(rootNode, targetNode, this.heuristic);
        
        // Add path to output variable
        this.addPath(nodes, path, model.getTarget());
        
        // Calculate cost
        int cost = this.calculateCost(path) + 1;
        
        // If path is not empty, it means it found a path between the nodes!
        if(!path.isEmpty())
        {
            this.statistics.setPathFound(true, cost);
        }
        
        return cost;
    }
    
    private void executeAStar(Node rootNode, Node targetNode, HeuristicStrategy heuristic)
    {
        // Starts at root node
        Node currentNode = rootNode;
        
        // Set goals for current node
        currentNode.setLocalGoal(0.0);
        currentNode.setGlobalGoal(heuristic.computeHeuristic(rootNode.getTile(), targetNode.getTile()));
        
        // Priority Queue of unvisited nodes to make sure the Node with the smallest goals are popped first
        Queue<Node> notTestedNodes = new PriorityQueue<>();
        notTestedNodes.add(currentNode);
        
        // While there are still nodes to test and,
        // If we don't need an optimal solution (just a fast one) we just end after we found the target
        while (!notTestedNodes.isEmpty())
        {
            // If we rather an early exit for faster time rather knowing for a fact we found the shortest path
            if(!this.findOptimalSolution && currentNode == targetNode) break;
            
            // Remove visited nodes from queue
            while(!notTestedNodes.isEmpty() && notTestedNodes.peek().isIsVisited())
                notTestedNodes.poll();
            
            // Exit loop if queue is empty
            if(notTestedNodes.isEmpty())
                break;
            
            currentNode = notTestedNodes.poll();
            this.statistics.incrementVisited();
            painter.drawTile(currentNode.getTile(), rootNode.getTile(), targetNode.getTile(), Tile.Type.HIGHLIGHT, 2);
            currentNode.setIsVisited(true);
            
            for(Node nodeNeighbor : currentNode.getNeighbors())
            {
                // If it has not been visited and it's not a wall, add it to not tested nodes... to be tested in consequent iterations
                if(!nodeNeighbor.isIsVisited() && !nodeNeighbor.isWall())
                    notTestedNodes.add(nodeNeighbor);
                
                // Potential lowest parent distance. We only need to get the neighbor weight becasue we're sure there's no Tile between them (which is the definition of them being neighbors)
                double goal = currentNode.getLocalGoal() + nodeNeighbor.getTile().getWeight(); 
                
                // If this goal is less than the current localGoal, update this neighbor with the currentNode as parent
                if(goal < nodeNeighbor.getLocalGoal())
                {
                    nodeNeighbor.setParent(currentNode);
                    nodeNeighbor.setLocalGoal(goal);
                    
                    // Use the established heuristic to set a global bias thowards the best possible path
                    // Nodes are eventually ordered by their globalGoal, so the priority queue will handle wich nodes are more important
                    nodeNeighbor.setGlobalGoal(nodeNeighbor.getLocalGoal() + heuristic.computeHeuristic(nodeNeighbor.getTile(), targetNode.getTile()));
                }
            }
            painter.drawTile(currentNode.getTile(), rootNode.getTile(), targetNode.getTile(), Tile.Type.VISITED, 0);
        }
    }
    
    /**
     * Calculates the cost of a given path with the weight of each tile
     * @param path 
     * @return cost of the path
     */
    private int calculateCost(List<Tile> path)
    {
        int total = -1;
        total = path.stream().map((tile) -> tile.getWeight()).reduce(total, Integer::sum);
        return total;
    }
    
    /**
     * Returns the path found by the AStar algorithm.
     * This method is to be called only after the AStar algorithm 
     * @return 
     */
    private List<Tile> addPath(List<Node> nodes, List<Tile> path, Tile target)
    {
        Node parentNode = null;
        
        // Get target from nodes list
        for(Node node : nodes)
        {
            if(node.getTile() == target)
            {
                parentNode = node;
                break;
            }
        }
        
        // Add parents to path, until we reached root (which does not have a parent)
        while(parentNode != null)
        {
            path.add(parentNode.getTile());
            parentNode = parentNode.getParent();
        }
        
        path.remove(path.size() - 1);
        
        // To make root the starting node
        Collections.reverse(path);
        
        return path;
    }
    
    /**
     * Helper class that represents a Tile with extra information relevant for this algorithm
     */
    private class Node implements Comparable<Node>
    {
        private final Tile tile;
        private final List<Node> neighbors;
        private Node parent;
        private boolean isVisited;
        private double globalGoal;
        private double localGoal;
        
        public Node(Tile tile)
        {
            this.tile = tile;
            this.parent = null;
            this.neighbors = new ArrayList<>();
            this.isVisited = false;
            this.globalGoal = Double.MAX_VALUE; 
            this.localGoal = Double.MAX_VALUE;
        }
        
        /**
         * Returns global goal for this node
         * @return double
         */
        public double getGlobalGoal()
        {
            return globalGoal;
        }

        /**
         * Defines the global goal for this node
         * @param globalGoal 
         */
        public void setGlobalGoal(double globalGoal)
        {
            this.globalGoal = globalGoal;
        }
        
        /**
         * Returns the local goal for this node
         * @return double
         */
        public double getLocalGoal()
        {
            return localGoal;
        }

        /**
         * Defines the local goal for this node
         * @param localGoal 
         */
        public void setLocalGoal(double localGoal)
        {
            this.localGoal = localGoal;
        }

        /**
         * Returns a boolean value representing if this node was already visited
         * @return 
         */
        public boolean isIsVisited()
        {
            return isVisited;
        }

        /**
         * Defines if the node was visited
         * @param isVisited 
         */
        public void setIsVisited(boolean isVisited)
        {
            this.isVisited = isVisited;
        }

        /**
         * Returns a list of neighbors for this node
         * @return List<Node> of neighbors
         */
        public List<Node> getNeighbors()
        {
            return neighbors;
        }

        /**
         * Sets this node's neighbors
         * @param nodes list of nodes to add to this node's neighbors
         * @param model of the grid containing all the data about the nodes
         */
        public void setNeighbors(List<Node> nodes, Grid model)
        {
            List<Tile> tileNeighbors = model.getTileNeighbors(this.getTile());

            for(Node node : nodes)
            {
                if(tileNeighbors.contains(node.getTile()))
                {
                    this.neighbors.add(node);
                }
            }
        }

        /**
         * Returns this node's tile
         * @return 
         */
        public Tile getTile()
        {
            return tile;
        }
        
        /**
         * Returns this node's parent node
         * @return 
         */
        public Node getParent()
        {
            return parent;
        }

        /**
         * Sets this node's parent
         * @param parent 
         */
        public void setParent(Node parent)
        {
            this.parent = parent;
        }
        
        /**
         * Returns a boolean value representing weather or not this node is a wall
         * @return 
         */
        public boolean isWall()
        {
            return this.tile.isWall();
        }
        
        /**
         * Returns x coordinate for this node
         * @return 
         */
        public int getX()
        {
            return this.tile.getX();
        }
        
        /**
         * Returns y coordinate for this node
         * @return 
         */
        public int getY()
        {
            return this.tile.getY();
        }

        @Override
        public int compareTo(Node other)
        {
            if(this.getGlobalGoal() < other.getGlobalGoal())
                return -1;
            if(this.getGlobalGoal() > other.getGlobalGoal())
                return 1;
            return 0;
        }

        @Override
        public String toString()
        {
            return "Node{" + "parent=" + parent + ", isVisited=" + isVisited + ", globalGoal=" + globalGoal + '}';
        }
    }
}