package fr.inria.edelweiss.kgraph.query;

import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.cg.datatype.DatatypeMap;
import fr.inria.acacia.corese.triple.parser.ASTQuery;
import fr.inria.acacia.corese.triple.parser.Constant;
import fr.inria.acacia.corese.triple.parser.Expression;
import fr.inria.acacia.corese.triple.parser.NSManager;
import fr.inria.acacia.corese.triple.parser.Term;
import fr.inria.edelweiss.kgenv.parser.Pragma;
import fr.inria.edelweiss.kgram.api.core.Expr;
import fr.inria.edelweiss.kgram.api.core.ExprType;
import fr.inria.edelweiss.kgram.api.core.Node;
import fr.inria.edelweiss.kgram.api.query.Environment;
import fr.inria.edelweiss.kgram.api.query.Producer;
import fr.inria.edelweiss.kgram.core.Mappings;
import fr.inria.edelweiss.kgram.core.Query;
import fr.inria.edelweiss.kgram.filter.Extension;
import fr.inria.edelweiss.kgraph.core.Graph;
import fr.inria.edelweiss.kgtool.load.LoadException;
import fr.inria.edelweiss.kgtool.transform.Transformer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * Plugin to Transformer
 * 
 * @author Olivier Corby, Wimmics INRIA I3S, 2015
 *
 */
public class PluginTransform implements ExprType {
    static Logger logger = Logger.getLogger(PluginTransform.class);

    protected IDatatype EMPTY = DatatypeMap.newStringBuilder("");
    private static final String VISIT_DEFAULT_NAME = NSManager.STL + "default";
    private static final IDatatype VISIT_DEFAULT = DatatypeMap.newResource(VISIT_DEFAULT_NAME);
    static final IDatatype TRUE = PluginImpl.TRUE;
    static final IDatatype FALSE = PluginImpl.FALSE;
    PluginImpl plugin;
    private int index = 0;

    PluginTransform(PluginImpl p) {
        plugin = p;
    }

    public Object function(Expr exp, Environment env, Producer p) {

        switch (exp.oper()) {
            
            case STL_NUMBER:
                return plugin.getValue(1 + env.count());

            case LEVEL:
                return getLevel(env, p);

            case STL_NL:
                return nl(null, env, p);

            case STL_ISSTART:
                return isStart(env, p);

            case STL_VISITED:
                return visited(exp, env, p);

            case PROLOG:
                return prolog(null, env, p);

            case STL_PREFIX:
                return prefix(env, p); 

            case STL_INDEX:
                return index(exp, env, p);

            case APPLY_TEMPLATES_ALL:
            case APPLY_TEMPLATES:
                return transform(null, null, null, exp, env, p);

            case FOCUS_NODE:
                return getFocusNode(null, env);



        }

        return null;
    }

    public Object function(Expr exp, Environment env, Producer p, IDatatype dt) {

        switch (exp.oper()) {


            case INDENT:
                return indent(dt, env, p);

            case STL_NL:
                return nl(dt, env, p);

            case PROLOG:
                return prolog(dt, env, p);

//            case STL_PROCESS:
//                return eval(exp, env, p, dt); 
                
            case STL_FUTURE:
                return dt;

            case STL_GET:
                return get(exp, env, p, dt);

            case STL_BOOLEAN:
                return bool(exp, env, p, dt);

            case STL_VISITED:
                return visited(exp, env, p, dt);
                
            case STL_ERRORS:
                return errors(exp, env, p, dt);

            case STL_VISIT:
                return visit(exp, env, p, null, dt, null);

            case APPLY_TEMPLATES:
            case APPLY_TEMPLATES_ALL:
                return transform(dt, null, null, null, exp, env, p);

            case CALL_TEMPLATE:
            case STL_TEMPLATE:
                return transform(null, dt, null, exp, env, p);

            case APPLY_TEMPLATES_WITH:
            case APPLY_TEMPLATES_WITH_ALL:
                return transform(dt, null, null, exp, env, p);

            case APPLY_TEMPLATES_GRAPH:
            case APPLY_TEMPLATES_NOGRAPH:
                return transform(null, null, dt, exp, env, p);

            case TURTLE:
                return turtle(dt, env, p);

            case PPURI:
            case URILITERAL:
            case XSDLITERAL:
                return uri(exp, dt, env, p);

            case STL_LOAD:
                load(dt, env, p);
                return EMPTY;


            case VISITED:
                return visited(dt, env, p);
                        

        }
        return null;
    }
    
    
        public Object function(Expr exp, Environment env, Producer p, IDatatype dt1, IDatatype dt2) {
            switch(exp.oper()){
                
            case APPLY_TEMPLATES:
            case APPLY_TEMPLATES_ALL:
                // dt1: focus
                // dt2: arg
                return transform(dt1, dt2, null, null, exp, env, p);
                               
            case APPLY_TEMPLATES_WITH_GRAPH:
            case APPLY_TEMPLATES_WITH_NOGRAPH:
                // dt1: transformation 
                // dt2: graph            
                return transform(dt1, null, dt2, exp, env, p);

            case APPLY_TEMPLATES_WITH:
            case APPLY_TEMPLATES_WITH_ALL:
                // dt1: transformation
                // dt2: focus
                
               return transform(dt2, null, dt1, null, exp, env, p);

            case CALL_TEMPLATE:
            case STL_TEMPLATE:
                // dt1: template name
                // dt2: focus
                return transform(dt2, null, null, dt1, exp, env, p);

            case CALL_TEMPLATE_WITH:
                // dt1: transformation
                // dt2: template name
                return transform(dt1, dt2, null, exp, env, p);
                
              case TURTLE:
                return turtle(dt1, dt2, env, p);     
                
            case STL_SET:                
            case STL_EXPORT:
                return set(exp, env, p, dt1, dt2);
                
            case STL_VGET:
                return vget(exp, env, p, dt1, dt2);    
                
           case STL_VISIT:
                return visit(exp, env, p, dt1, dt2, null);
                
            case STL_GET:
                return get(exp, env, p, dt1, dt2);   
                 
    
        }
            return null;
        }
    
    
    
    public Object eval(Expr exp, Environment env, Producer p, Object[] args) {
        IDatatype[] param = (IDatatype[]) args;
        switch (exp.oper()){
            
           case STL_PROCESS:
                return processDef(exp, env, p, param);     
        }

        IDatatype dt1 =  param[0];
        IDatatype dt2 =  param[1];
        IDatatype dt3 =  param[2];

        switch (exp.oper()) {

         
            case CALL_TEMPLATE:
            case STL_TEMPLATE:
                // dt1: template name
                // dt2: focus
                // dt3: arg
                return transform(getArgs(param, 1), dt2, dt3, null, dt1, null, exp, env, p);

            case CALL_TEMPLATE_WITH:
                // dt1: transformation
                // dt2: template name
                // dt3: focus
                return transform(getArgs(param, 2), dt3, null, dt1, dt2, null, exp, env, p);

            case APPLY_TEMPLATES_WITH:
            case APPLY_TEMPLATES_WITH_ALL:
                // dt1: transformation
                // dt2: focus
                // dt3: arg
                
               return transform(getArgs(param, 1), dt2, dt3, dt1, null, null, exp, env, p);
                
            case APPLY_TEMPLATES:
            case APPLY_TEMPLATES_ALL:
                // dt1: focus
                // dt2: arg
                return transform(getArgs(param, 0), dt1, dt2, null, null, null, exp, env, p);  
                
            case APPLY_TEMPLATES_WITH_GRAPH:
            case APPLY_TEMPLATES_WITH_NOGRAPH:
                // dt1: transformation 
                // dt2: graph 
                // dt3; focus
                return transform(getArgs(param, 2), dt3, null, dt1, null, dt2, exp, env, p);
                
            case STL_VISIT:
                return visit(exp, env, p, dt1, dt2, dt3);
                
            case STL_VSET:
                return vset(exp, env, p, dt1, dt2, dt3); 
                           
        }

        return null;
    }
    
    
    
    
    IDatatype[] getArgs(IDatatype[] obj, int n){
        return Arrays.copyOfRange(obj, n, obj.length);
    }
    
    
    

    private IDatatype bool(Expr exp, Environment env, Producer p, IDatatype dt) {
        if (dt.stringValue().contains("false")) {
            return FALSE;
        }
        return TRUE;
    }

    Transformer getTransformer(Environment env, Producer p) {
        return getTransformer(null, env, p, (IDatatype) null, (IDatatype) null, null);
    }

//    Transformer getTransformer(Expr exp, Environment env, Producer prod, IDatatype trans) {
//        return getTransformer(exp, env, prod, trans, null);
//    }

    /**
     * uri: transformation URI 
     * gname: named graph 
     * If uri == null, get current transformer
     * TODO: cache for named graph
     */
    Transformer getTransformer(Expr exp, Environment env, Producer prod, IDatatype uri, IDatatype temp, String gname) {
        Query q = env.getQuery();
        ASTQuery ast = (ASTQuery) q.getAST();
        String transform = getTrans(uri, temp);
        
        // @deprecated:
        if (transform == null && q.hasPragma(Pragma.TEMPLATE)) {
            transform = (String) q.getPragma(Pragma.TEMPLATE);
        }

        Transformer t = (Transformer) q.getTransformer(transform);

        if (transform == null && t != null) {
            transform = t.getTransformation();
        }

        if (gname != null) {
            // transform named graph
            try {
                boolean with = (exp == null) ? true
                        : exp.oper() == ExprType.APPLY_TEMPLATES_GRAPH
                        || exp.oper() == ExprType.APPLY_TEMPLATES_WITH_GRAPH;

                Transformer gt = Transformer.create((Graph) prod.getGraph(), transform, gname, with);
                complete(q, gt, uri);

//                if (t == null) {
//                    // get current transformer if any to get its NSManager 
//                    t = (Transformer) q.getTransformer();
//                }
                t = gt;
            } catch (LoadException ex) {
                logger.error(ex);
                t = Transformer.create(Graph.create(), null);
            }
        } else if (t == null) {
            t = Transformer.create(prod, transform);
            complete(q, t, uri);
            q.setTransformer(transform, t);
        } else {
            Graph g = t.getGraph();
            if (g != prod.getGraph()) {
                // Transformer exist but with another graph
                // create a new one
                t = Transformer.create(prod, transform);
                complete(q, t, uri);
            }
        }

        return t;
    }
    

    void complete(Query q, Transformer t, IDatatype uri) {
        // set after init
        //t.init((ASTQuery) q.getAST());
        // TODO: uri vs transform ???
        t.set(Transformer.STL_TRANSFORM, uri);
        t.complete(q, (Transformer) q.getTransformer());
    }

    /**
     * Increment indentation level
     */
    IDatatype indent(IDatatype dt, Environment env, Producer prod) {
        Transformer t = getTransformer(env, prod);
        t.setLevel(t.getLevel() + dt.intValue());
        return EMPTY;
    }

    /**
     * New Line with indentation given by t.getLevel() Increment level if
     * dt!=null
     */
    IDatatype nl(IDatatype dt, Environment env, Producer prod) {
        Transformer t = getTransformer(env, prod);
        if (dt != null) {
            t.setLevel(t.getLevel() + dt.intValue());
        }
        return t.tabulate();
    }

    IDatatype prolog(IDatatype dt, Environment env, Producer prod) {
        Transformer p = getTransformer(env, prod);
        String title = null;
        if (dt != null) {
            title = dt.getLabel();
        }
        String pref = p.getNSM().toString(title);
        return plugin.getValue(pref);
    }

    Mappings prefix(Environment env, Producer prod) {
        Transformer p = getTransformer(env, prod);
        return p.NSMtoMappings();
    }

    IDatatype isStart(Environment env, Producer prod) {
        Transformer p = getTransformer(env, prod);
        boolean b = p.isStart();
        return plugin.getValue(b);
    }
    
    String getLabel(IDatatype dt) {
        if (dt == null) {
            return null;
        }
        return dt.getLabel();
    }
    
    String getTransform(IDatatype trans, IDatatype temp){
        if (trans == null && temp == null){
             return null;
        }       
        IDatatype dt = (trans != null) ? trans : temp;
        return Transformer.getURI(dt.getLabel());            
    }
    
    String getTemplate(IDatatype trans, IDatatype temp){
        if (temp != null){
            return getLabel(temp);
        }
        if (trans != null){
            return Transformer.getStartName(trans.getLabel());
        }
        return null;
    }
    
    String getTrans(IDatatype trans, IDatatype temp){
       return getLabel(trans);    
    }
    
    String getTemp(IDatatype trans, IDatatype temp){
        return getLabel(temp);
    }

    /**
     * Without focus node
     */
    IDatatype transform(IDatatype trans, IDatatype temp, IDatatype name, Expr exp, Environment env, Producer prod) {
        Transformer p = getTransformer(exp, env, prod, trans, temp, getLabel(name));
        return p.process(getTemp(trans, temp),
                exp.oper() == ExprType.APPLY_TEMPLATES_ALL
                || exp.oper() == ExprType.APPLY_TEMPLATES_WITH_ALL,
                exp.getModality());
    }

   
    IDatatype transform(IDatatype focus, IDatatype arg, IDatatype trans, IDatatype temp, Expr exp, Environment env, Producer prod) {
        return transform(null, focus, arg, trans, temp, null, exp, env, prod);
    }

    /**
     * exp:   calling expression eg st:apply-templates
     * focus: focus node, arg is an argument node
     * trans: transformation URI, may be null
     * temp:  name of a named template, may be null
     * name:  named graph
     */    
    IDatatype transform(IDatatype[] args, IDatatype focus, IDatatype arg, IDatatype trans, IDatatype temp, IDatatype name,
            Expr exp, Environment env, Producer prod) {
        Transformer p = getTransformer(exp, env, prod, trans, temp, getLabel(name));
        IDatatype dt = p.process(args, focus, arg,
                getTemp(trans, temp),
                exp.oper() == ExprType.APPLY_TEMPLATES_ALL
                || exp.oper() == ExprType.APPLY_TEMPLATES_WITH_ALL,
                exp.getModality(), exp, env.getQuery());
        return dt;
    }

    /**
     * st:process(var) : default variable processing by SPARQL Template Ask
     * Transformer what is default behavior set st:process() to it's default
     * behavior the default behavior is st:turtle
     */
    public Object processDef(Expr exp, Environment env, Producer p, IDatatype[] args) {
        Extension ext = env.getQuery().getExtension();
        if (ext != null && ext.isDefined(exp)) {
            return plugin.getEvaluator().eval(exp, env, p, args, ext);
        }

        Transformer pp = getTransformer(env, p);
        int oper = pp.getProcess();

        // overload current st:process() oper code to default behaviour oper code
        // future executions of this st:process() will directly execute target default behavior
        exp.setOper(oper);
        Object res = plugin.function(exp, env, p,  args[0]);
        return res;

    }
  
 
    /**
     * exp = st:aggregate(?out)
     * Overload exp with actual transformer aggregate
     * May be defined in template st:profile 
     * using st:define(st:aggregate(?x) = st:agg_and(?x)
     * otherwise, default is st:group_concat
     */
     Expr decode(Expr exp, Environment env, Producer p){
        Query q = env.getQuery();
        Extension ext = q.getExtension();
        Transformer t = getTransformer(env, p);
        
        switch (exp.oper()){
             case STL_AGGREGATE:
                 // possibly defined in template st:profile
                 Expr def = null;
                 if (ext != null){
                     def = ext.get(exp);
                     //q.getProfile(Transformer.STL_AGGREGATE);
                 }
                 // default aggregate
                 int oper = t.getAggregate();
                 exp = decode(exp, def, oper);
         }
        
         return exp;
     }
     
     
     /**
      * def = st:aggregate(?x) = st:agg_and(?x)
      * Overload exp operator with current transformer operator
      */
     Expr decode(Expr exp, Expr def, int oper){
         if (def != null){
             oper = def.getBody().oper(); //getExp(1).oper();
         }
         exp.setOper(oper);
         return exp;        
     }
    
  
    public IDatatype get(Expr exp, Environment env, Producer p, IDatatype dt) {
        return get(exp, env, p, dt.getLabel());
    }

    public IDatatype get(Expr exp, Environment env, Producer p, String name) {
        Transformer t = getTransformer(env, p);
        return t.get(name);
    }

    public IDatatype get(Expr exp, Environment env, Producer p, IDatatype dt1, IDatatype dt2) {
        Transformer t = getTransformer(env, p);
        IDatatype dt = get(exp, env, p, dt1);
        if (dt == null) {
            return FALSE;
        }
        boolean b = dt.equals(dt2);
        return plugin.getValue(b);
    }

    public IDatatype index(Expr exp, Environment env, Producer p) {
        return plugin.getValue(index++);
    }

    public Object set(Expr exp, Environment env, Producer p, IDatatype dt1, IDatatype dt2) {
        Transformer t = getTransformer(env, p);
        if (exp.oper() == STL_SET){
          t.set(dt1.getLabel(), dt2);
        }
        else {
           t.export(dt1.getLabel(), dt2); 
        }
        return dt2;
    }

    public IDatatype vset(Expr exp, Environment env, Producer p, IDatatype dt1, IDatatype dt2, IDatatype dt3) {
        Transformer t = getTransformer(env, p);
        return t.vset(dt1, dt2, dt3);
    }

    public IDatatype vget(Expr exp, Environment env, Producer p, IDatatype dt1, IDatatype dt2) {
        Transformer t = getTransformer(env, p);
        return t.vget(dt1, dt2);
    }

    public Object visited(Expr exp, Environment env, Producer p) {
        Transformer t = getTransformer(env, p);
        Collection<IDatatype> list = t.visited();
        return DatatypeMap.createObject("list", list);
    }

    public Object visited(Expr exp, Environment env, Producer p, IDatatype dt) {
        Transformer t = getTransformer(env, p);
        boolean b = t.visited(dt);
        return plugin.getValue(b);
    }
    
     public IDatatype errors(Expr exp, Environment env, Producer p, IDatatype dt) {
        Transformer t = getTransformer(env, p);
	return DatatypeMap.createList(t.getVisitor().getErrors(dt));
    }

    // Visitor design pattern
    public Object visit(Expr exp, Environment env, Producer p, IDatatype dt1, IDatatype dt2, IDatatype dt3) {
        Transformer t = getTransformer(env, p);
        if (dt1 == null) {
            dt1 = VISIT_DEFAULT;
        }
        t.visit(dt1, dt2, dt3);
        return TRUE;
    }

    IDatatype visited(IDatatype dt, Environment env, Producer p) {
        Transformer pp = getTransformer(env, p);
        boolean b = pp.isVisited(dt);
        return plugin.getValue(b);
    }

    /**
     * proc: st:process(?y) def: st:process(?x) = st:apply-templates(?x) copy
     * def right exp and rename its variable (?x) as proc variable (?y) PRAGMA:
     * do no process exists {} in def
     */
//    Expr rewrite(Expr proc, Expr def, ASTQuery ast) {
//        Term tproc = (Term) proc;
//        Term tdef = (Term) def;
//        Variable v1 = tdef.getArg(0).getArg(0).getVariable(); // ?x
//        Variable v2 = tproc.getArg(0).getVariable(); // ?y
//        // replace ?x by ?y
//        Expression tt = tdef.getArg(1).copy(v1, v2);
//        tt.compile(ast);
//        return tt;
//    }
    
    
     /**
     * create concat(str, st:number(), str)
     */
    public Expr createFunction(String name, List<Object> args, Environment env){
        Term t = Term.function(name);
        for (Object arg : args){
            if (arg instanceof IDatatype){
                // str: arg is a StringBuilder, keep it as is
                Constant cst = Constant.create("Future", null, null);
                cst.setDatatypeValue((IDatatype) arg);
                t.add(cst);
            }
            else {
                // st:number()
               t.add((Expression) arg);
            }
        }
        t.compile((ASTQuery)env.getQuery().getAST());
        return t;
    }
    

    /**
     *
     *
     */
    IDatatype turtle(IDatatype o, IDatatype o2, Environment env, Producer prod) {
        Transformer p = getTransformer(env, prod);
        IDatatype dt = p.turtle(o, o2.equals(TRUE));
        return dt;
    }

    IDatatype turtle(IDatatype o, Environment env, Producer prod) {
        Transformer p = getTransformer(env, prod);
        IDatatype dt = p.turtle(o);
        return dt;
    }

    IDatatype xsdLiteral(IDatatype o, Environment env, Producer prod) {
        Transformer p = getTransformer(env, prod);
        IDatatype dt = p.xsdLiteral(o);
        return dt;
    }

    /**
     * @deprecated
     */
    IDatatype uri(Expr exp, IDatatype dt, Environment env, Producer prod) {
        if (dt.isURI()) {
            return turtle(dt, env, prod);
        } else if (dt.isLiteral() && exp.oper() == ExprType.URILITERAL) {
            return turtle(dt, env, prod);
        } else if (dt.isLiteral() && exp.oper() == ExprType.XSDLITERAL) {
            return xsdLiteral(dt, env, prod);
        } else {
            return transform(dt, null, null, null, exp, env, prod);
        }
    }

    IDatatype getLevel(Environment env, Producer prod) {
        return plugin.getValue(level(env, prod));
    }

    int level(Environment env, Producer prod) {
        Transformer p = getTransformer(env, prod);
        return p.level();
    }

    void load(IDatatype dt, Environment env, Producer p) {
        Transformer t = getTransformer(env, p);
        t.load(dt.getLabel());
    }

    private Object getFocusNode(IDatatype dt, Environment env) {
        String name = Transformer.IN;
        if (dt != null) {
            name = dt.getLabel();
        }
        Node node = env.getNode(name);
        if (node == null) {
            return null;
        }
        return node.getValue();
    }
     
}
