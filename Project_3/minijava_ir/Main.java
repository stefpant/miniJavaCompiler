import syntaxtree.*;
import visitor.*;
import java.io.*;

class Main {
    public static void main (String [] args){
    	if(args.length < 1){
	        System.err.println("Usage: java Driver <inputFile1> ... <inputFileN>");
	        System.exit(1);
	    }
	    FileInputStream fis = null;
        //PrintStream originalOut = System.out;
        for(int i=0; i<args.length; i++){
	        try{
                //System.setOut(originalOut);
                String path = args[i].substring(0, args[i].length() - 4);//remove from name the 'java'
                path = path + "ll";
                PrintStream new_out = new PrintStream(new FileOutputStream(path));
                //System.out.println(path);
	            fis = new FileInputStream(args[i]);
	            MiniJavaParser parser = new MiniJavaParser(fis);
                Goal root = parser.Goal();
	            //System.out.println("Program "+ args[i] +": parsed successfully.");
	            MyFillSTVisitor fv = new MyFillSTVisitor();
                SymbolTable myST = new SymbolTable();
	            root.accept(fv, myST);
                myST.checkReferredTypes();
                //myST.print();
                TypeCheckVisitor tcv = new TypeCheckVisitor();
                root.accept(tcv, myST);
	            //System.out.println("Program "+ args[i] +": Static Checking OK.");
                System.setOut(new_out);
                //myST.printOffsets();
                IntRepVisitor irv = new IntRepVisitor();
                root.accept(irv, myST);
                
	        }
	        catch(ParseException ex){
	            System.err.println(ex.getMessage());
	        }
	        catch(FileNotFoundException ex){
	            System.err.println(ex.getMessage());
	        }
            catch(Exception ex){
	            System.err.println(ex.getMessage());
            }
	        finally{
	            try{
	            	if(fis != null) fis.close();
	            }
	            catch(IOException ex){
	            	System.err.println(ex.getMessage());
	            }
	        }
            fis = null;
        }
    }
}
