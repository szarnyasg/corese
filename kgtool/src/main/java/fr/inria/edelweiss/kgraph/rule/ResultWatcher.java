/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.inria.edelweiss.kgraph.rule;

import fr.inria.edelweiss.kgram.api.core.Edge;
import fr.inria.edelweiss.kgram.api.core.Entity;
import fr.inria.edelweiss.kgram.api.core.ExpType;
import fr.inria.edelweiss.kgram.api.core.Expr;
import fr.inria.edelweiss.kgram.api.core.Node;
import fr.inria.edelweiss.kgram.api.core.Regex;
import fr.inria.edelweiss.kgram.api.query.Environment;
import fr.inria.edelweiss.kgram.core.Distinct;
import fr.inria.edelweiss.kgram.core.Exp;
import fr.inria.edelweiss.kgram.core.Mappings;
import fr.inria.edelweiss.kgram.core.Query;
import fr.inria.edelweiss.kgram.event.ResultListener;
import fr.inria.edelweiss.kgram.path.Path;
import fr.inria.edelweiss.kgraph.api.GraphListener;
import fr.inria.edelweiss.kgraph.core.Graph;
import fr.inria.edelweiss.kgraph.logic.RDFS;
import fr.inria.edelweiss.kgraph.query.Construct;
import java.util.ArrayList;
import java.util.List;

/**
 * Watch kgram query solutions of rules for RuleEngine
 * 1 Check that a solution at loop n contains at least one 
 * edge deduced at loop n-1 (that is a new edge)
 * 2 Eliminate duplicate solution by a select distinct * on construct variables
 * 3 Create Edge directly (without Mapping created)
 * Does not optimize with:
 * exists {}
 * Property Path
 * subquery
 * 
 * @author Olivier Corby, Wimmics Inria I3S, 2014
 *
 */
public class ResultWatcher implements ResultListener, GraphListener {   
    // proportion of new edges under which we may consider only new edges
    public static double LIMIT = 0.2;
    // max sum of proportion of new edges
    public static double TOTAL = 0.3;
      
    int loop = 0, ruleLoop = 0;
    int cpos = 0, cneg = 0;
    int cnode = 0;
    boolean isWatch = true, start = true;
    private boolean isDistinct = true;
    private boolean isSkipPath = false;
    private boolean test = false;
    boolean isNew = false;
    int index = -1;
    
    Construct cons;
    Mappings map;
    Rule rule;
    Graph graph;
    Distinct dist;
    ArrayList<Entity> list;
    private boolean trace;
    private boolean isTestable;
    
    ResultWatcher(Graph g){
        graph = g;  
        list = new ArrayList<Entity>();
    }
    
    public Distinct getDistinct(){
        return dist;
    }
    
    void setConstruct(Construct c){
        cons = c;
    }
    
    void setMappings(Mappings m){
        map = m;
    }
    
    void setLoop(int n){
        ruleLoop = n;
    }
    
    void start(int n){
        loop = n;
    }

    void start(Rule r){
        rule = r;
        isWatch = doit(true);
        isNew = false;
        start = true;
        isTestable = false;
        init(r);
    }
    
   /**
    * (1) If there is only one predicate in where with new edges
    * (Only one occurrence of this predicate in where)
    * We can focus on these new edge using listen(Edge, Entity)
    * (2) If there are two predicates and the proportion of new triples is small enough < 0.3
    * we will focus on new triples using a specific Index of edges sorted by timestamp
    * (see union())
    */
    void start(Record ot, Record nt) {
        setLoop(ot.getIndex());

        if (nt.getCount() == 1 
                && rule.getQuery().nbPredicate(nt.getPredicate()) == 1) {
            index = rule.getQuery().getEdge(nt.getPredicate()).getIndex();
            if (index != -1) {
                isNew = doit(true);
            }            
        }
        else if (loop > 0 && rule.getQuery().getEdgeList() == null) {
            int n = 0;
            boolean ok = true;
            double tt = 0.0;
            // new edges <= 30% edges
            for (Node pred : rule.getPredicates()) {
                if (nt.get(pred) > ot.get(pred)) {
                    n++;
                    // proportion of new edges
                    double dd = ((double) (nt.get(pred) - ot.get(pred))) / (double) nt.get(pred);
                    // sum of proportion of new edges
                    tt += dd;
                }
            }
                    
            // 2 edges and may be 1 a filter
            if (n <= 2 && tt < TOTAL){// && ok){
                Exp body = rule.getQuery().getBody();
                int ne = 0, nf = 0;
                
                for (Exp exp : body){
                    if (exp.isEdge() && exp.getEdge().getEdgeVariable() == null){
                        ne++;
                    }
                    else if (exp.isFilter()){
                        nf++;
                    }
                }
                
                if (ne == 2 && nf <= 1){
                    isTestable = doit(true);
                }
            }
        }
    }
    
    /**
     * set up a distinct * on construct variables
     * hence do not apply rule twice on same solution
     */
    void init(Rule r){
        r.getQuery().setEdgeList(null);
        List<Node> list = r.getQuery().getConstructNodes();
        if (list != null && ! list.isEmpty()){
            dist = Distinct.create(list);
        }
    }
    
    void finish(Rule r){
        dist = null;
    }
    
    /**
     * Environment contain a candidate solution
     * Check that environment contains at least one new edge from preceding 
     * RuleEngine loop 
     * Check that this solution is not duplicate: select distinct * on construct variables
     * This function is called by kgram just before returning a solution
     */
    @Override
    public boolean process(Environment env) {
        if (!isWatch) {
            return store(env);
        }

        if (loop == 0 || isNew) {
            return store(env);
        }

        for (Entity ent : env.getEdges()) {

            if (ent != null && ent.getEdge().getIndex() >= ruleLoop) {
                return store(env);
            }
        }

        cneg += 1;
        return false;
    }
    
    // return false to skip optimization
    boolean doit(boolean b){
        return b;
    }
    
    boolean store(Environment env){
        if (isDistinct && dist != null){
            // select distinct * on construct variables
            if (! dist.isDistinct(env)){  
                cneg += 1;
                return false;
            }
        }
        cpos+= 1;
        if (cons == null){
            // Mapping created by kgram
            return true;
        }
        else {
            // create Edge 
            // no Mapping created by kgram
          cons.construct(map, env, null);
           return false;
        }
    }

    @Override
    public boolean process(Path path) {
        return true;
    }

    @Override
    public boolean enter(Entity ent, Regex exp, int size) {
         return true;
   }

    @Override
    public boolean leave(Entity ent, Regex exp, int size) {
         return true;
   }

    @Override
    public Exp listen(Exp exp, int n) {
        switch (exp.type()) {
            
            case Exp.PATH:
                if (isSkipPath){
                    // skip path to check if a solution has new edges
                }
                else {
                    // do not skip path to check if a solution has new edges
                   isWatch = false;   
                }
                break;
                
            case Exp.QUERY:
                isWatch = false;
                break;
                
            case Exp.UNION:
            case Exp.OPTION:
                // because we may not go through the branch with new edges
                // check new at the end as usual
                isNew = false;
                break;
        }

        if (n == 0 && exp.type() == Exp.AND){
                
            if (rule.isGTransitive() && rule.getQuery().getEdgeList() != null) {
                // exp = where { ?p a owl:TransitiveProperty . ?x ?p ?y . ?y ?p ?z }
                // there is a list of candidates for ?x ?p ?y
                // skip first query edge: skip exp.get(0)
                exp = Exp.create(Exp.AND, exp.get(1), exp.get(2));
            }
            else if (isTestable && graph.hasList()){
                // focus on new edges in a specific graph Index sorted by timestamps
                    isTestable = false;
                    exp = union(exp);
            }
            
        }

        return exp;
    }
    
    /**
     * exp = 2 edge (and may be 1 filter)
     * return { getNew(e1) e2 } union { getNew(e2) e1 }
     * where getNew(e) is a directive to Producer to focus on new edges only
     * new edges are those with level(e) >= ruleLoop
     * There is a Graph Index where edges are sorted by level
     */
    Exp union(Exp exp){               
        int fst = 0, snd = 1, flt = 2;
        if (exp.size() == 3 && exp.get(1).isFilter()){
            snd = 2;
            flt = 1;
        }
                  
        Exp e1 = Exp.create(Exp.EDGE, exp.get(fst).getEdge());
        e1.setLevel(ruleLoop);
        
        Exp a1 = Exp.create(Exp.AND, e1, exp.get(snd));
        if (exp.size() == 3){
            a1.add(exp.get(flt));
        }
        
        Exp e2 = Exp.create(Exp.EDGE, exp.get(snd).getEdge());
        e2.setLevel(ruleLoop);
        
        Exp a2 = Exp.create(Exp.AND, e2, exp.get(fst));
        if (exp.size() == 3){
            a2.add(exp.get(flt));
        }        
        Exp ee = Exp.create(Exp.UNION, a1, a2);
               
        if (trace){
            System.out.println("Compile: " + ee);
        }

        return ee;
    }
 
    
     @Override
   public void listen(Expr exp) {
        switch (exp.type()){
            case ExpType.EXIST: 
                isWatch = false;
        }           
    }
     
     public String toString(){
         return "positive: " + cpos + "\n"
              + "negative: " + cneg;
     }
     
  
     
     
     
     
     /*********************************************************************
      * 
      * 
      */
     
     
     
    Exp compile(Exp exp){
        Exp e1    = Exp.create(Exp.EDGE, exp.get(1).getEdge());
        Exp e2    = Exp.create(Exp.EDGE, exp.get(0).getEdge());       
        Exp ee    = Exp.create(Exp.AND, e1, e2); 
        exp.get(0).setIndex(ruleLoop);
        e1.setIndex(ruleLoop);
        Exp union = Exp.create(Exp.UNION, exp, ee);
        return union;
    }
    
    
    
    /**************************************************************
     * 
     *  GraphListener
     * 
     *************************************************************/
    
    
    public void clear(){
        list.clear();
    }
    
    public List<Entity> getList(){
        return list;
    }

    @Override
    public void addSource(Graph g) {
    }

    @Override
    public boolean onInsert(Graph g, Entity ent) {
        return true;   
    }

    @Override
    public void insert(Graph g, Entity ent) {
        // TODO
        if (ent.getEdge().getLabel().equals(RDFS.SUBCLASSOF)){
            list.add(ent);
        }
    }

    @Override
    public void delete(Graph g, Entity ent) {
    }

    @Override
    public void start(Graph g, Query q) {
    }

    @Override
    public void finish(Graph g, Query q, Mappings m) {
    }

    @Override
    public void load(String path) {
    }

    /**
     * @return the isSkipPath
     */
    public boolean isSkipPath() {
        return isSkipPath;
    }

    /**
     * @param isSkipPath the isSkipPath to set
     */
    public void setSkipPath(boolean isSkipPath) {
        this.isSkipPath = isSkipPath;
    }

    /**
     * @return the isDistinct
     */
    public boolean isDistinct() {
        return isDistinct;
    }

    /**
     * @param isDistinct the isDistinct to set
     */
    public void setDistinct(boolean isDistinct) {
        this.isDistinct = isDistinct;
    }

    /**
     * @return the test
     */
    public boolean isTest() {
        return test;
    }

    /**
     * @param test the test to set
     */
    public void setTest(boolean test) {
        this.test = test;
    }

    @Override
    public boolean listen(Edge edge, Entity ent) {
        if (isNew  
                && edge.getIndex() == index 
                && ent.getEdge().getIndex() < ruleLoop){
             return false;           
        }
        return true;
    }

    /**
     * @return the trace
     */
    public boolean isTrace() {
        return trace;
    }

    /**
     * @param trace the trace to set
     */
    public void setTrace(boolean trace) {
        this.trace = trace;
    }
    
   
     public boolean isNew(){
         return isNew;
     }
     
     
     
     
     
}
