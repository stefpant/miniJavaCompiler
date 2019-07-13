import java_cup.runtime.*;
import java.io.*;

class MyStringParser {
    public static void main(String[] argv) throws Exception{
        //System.out.println("Please type your arithmethic expression:");
        Parser p = new Parser(new Scanner(new InputStreamReader(System.in)));
        p.parse();
    }
}
