/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package COMSOLmaster;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenetics.DoubleGene;
import org.jenetics.MeanAlterer;
import org.jenetics.Mutator;
import org.jenetics.Optimize;
import org.jenetics.Phenotype;
import org.jenetics.engine.Engine;
import org.jenetics.engine.EvolutionResult;
import static org.jenetics.engine.EvolutionResult.toBestPhenotype;
import org.jenetics.engine.EvolutionStatistics;
import org.jenetics.engine.codecs;
import static org.jenetics.engine.limit.bySteadyFitness;
import org.jenetics.util.DoubleRange;


/**
 *
 * @author Pedro Anchieta
 */
public class Optimizer {
        
        private static String classfile;
        private static final double RECT1_Y_MIN = 0;
        private static final double RECT1_Y_MAX = 1;
        private static final double RECT2_SIZE_Y_MIN = 0.5;
        private static final double RECT2_SIZE_Y_MAX = 10;
        private static final double RECT3_Y_MIN = 0;
        private static final double RECT3_Y_MAX = 1;
        private final double mutationProb; //0.03
        private final double xOver; //0.6
        private final int nThreads;
        private final int maxGens;
        private final int popSize;
                
    public Optimizer ()
    {
        mutationProb = 0.1;
        xOver = 0.05;
        nThreads = 1;
        maxGens = 10;
        popSize = 30;
        
    }

    private static double fitness(final double[] x) { /*NOT thread safe. Yet...*/
	
    try {
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "cd " + System.getProperty("user.dir"));
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            
            File fout = new File("log.txt");
            FileOutputStream fos = new FileOutputStream(fout);
                                
            while (true)
            {
                line = r.readLine();
                if (line == null) { break; }
                System.out.println(line);
            }
            double rect2X, rect1Ypos, rect3Ypos;
            
            rect1Ypos = rect3Ypos = x[1] - 0.1;
            rect1Ypos = rect1Ypos * x[0];
            rect3Ypos = rect3Ypos * x[2];
            rect2X = 5/x[1];
            
            builder.command("cmd.exe", "/c", "comsolbatch -inputfile " + classfile + " \"%cd%\" " + rect1Ypos + " " + rect2X + " " + x[1] + " " + rect3Ypos);
            p = builder.start();
            r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            
            try (BufferedWriter lg = new BufferedWriter(new OutputStreamWriter(fos)))
              {
                  while (true)
                    {
                        line = r.readLine();
                        if (line == null) { break; }
                        System.out.println(line);
                        lg.write(line);
                        lg.newLine();
                    }
                  lg.close();
              }
            catch(IOException e)
            {
                System.err.println("IOException: " + e.getMessage());
            }
            
            LogHandler lh = new LogHandler ("log.txt");
            
            if (lh.error()) //Handle error that was detected
            {
                System.out.println ("Error detected in COMSOL log file");
                 
            }                
              
 
            OutputReader or = new OutputReader("Velocity.txt");
            try(FileWriter fw = new FileWriter("conv.txt", true);
                    
            BufferedWriter bw = new BufferedWriter(fw);
                    
            PrintWriter out = new PrintWriter(bw))
            {
                out.println(rect1Ypos + " " + rect2X + " " + x[1] + " " + rect3Ypos + " ->" + or.avgSpeed()); /*Printing each fitness function to file conv.txt*/
            } 
            catch (IOException e) 
            {
                System.err.println("Error: " + e.getMessage());
            }
            
                              
            return or.avgSpeed();
        } 
    catch (IOException ex) 
        {
            Logger.getLogger(Optimizer.class.getName()).log(Level.SEVERE, null, ex);
        }
    return 1000;
}
    
    public void genetic(ExternalPrograms ep) throws IOException
    {
        ep.init();
        Optimizer.classfile = ep.getClassfile();
        
        
        final ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        final Engine<DoubleGene, Double> engine = Engine
			.builder(
				Optimizer::fitness,
				codecs.ofVector(DoubleRange.of(RECT1_Y_MIN,RECT1_Y_MAX),DoubleRange.of(RECT2_SIZE_Y_MIN,RECT2_SIZE_Y_MAX),DoubleRange.of(RECT3_Y_MIN,RECT3_Y_MAX)))
                        .populationSize(popSize)
                        .offspringFraction(0.5)
                        .optimize(Optimize.MAXIMUM)
			.alterers(
				new Mutator<>(mutationProb),
				new MeanAlterer<>(xOver))
                        .executor(executor)
                        .build();

		final EvolutionStatistics<Double, ?>
			statistics = EvolutionStatistics.ofNumber();

		/*final Phenotype<DoubleGene, Double> best = engine.stream()
			.limit(bySteadyFitness(3))
			.peek(statistics)
			.collect(toBestPhenotype());*/
                
                final EvolutionResult<DoubleGene, Double> best = engine.stream() 
                        .limit(bySteadyFitness (7))
                        .limit(300)
                        .peek(statistics)
                        .collect(EvolutionResult.toBestEvolutionResult()); /*Forcing elitism*/
                
                /*final EvolutionResult<DoubleGene, Double> best = engine.stream() 
                        .limit(maxGens)
                        .peek(statistics)
                        .collect(EvolutionResult.toBestEvolutionResult());*/

		System.out.println(statistics);
                System.out.println("Best= " + best.getBestPhenotype());
                
                /*System.out.println(engine.getSurvivorsSelector());*/
        
                
    }

 
}
