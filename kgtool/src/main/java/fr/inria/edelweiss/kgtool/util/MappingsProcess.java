/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.inria.edelweiss.kgtool.util;

import fr.inria.edelweiss.kgram.core.Mappings;
import fr.inria.edelweiss.kgraph.core.Graph;
import fr.inria.edelweiss.kgraph.logic.Entailment;
import fr.inria.edelweiss.kgtool.load.Load;
import fr.inria.edelweiss.kgtool.load.LoadException;
import fr.inria.edelweiss.kgtool.print.RDFResultFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2015
 *
 */
public class MappingsProcess {
    
    Mappings map;
    
    MappingsProcess (Mappings m){
        map = m;
    }
    
    public static MappingsProcess create(Mappings m){
        return new MappingsProcess(m);
    }
    
    public Graph getGraph() {
        RDFResultFormat f = RDFResultFormat.create(map);
        Graph g = Graph.create();
        Load ld = Load.create(g);
        try {
            ld.loadString(f.toString(), "", Entailment.DEFAULT, "", Load.TURTLE_FORMAT);
        } catch (LoadException ex) {
            Logger.getLogger(MappingsProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        return g;
    }

}
