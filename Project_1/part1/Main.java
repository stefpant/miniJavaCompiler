import java.io.IOException;

public class Main{

	public static void main(String[] args){
		try{
			Parser P = new Parser( System.in );
			int result = P.parse();
			System.out.println("result:" + result);
		}
		catch(IOException e){
			System.err.println(e.getMessage());
		}
		catch(ParseError err){
			System.err.println(err.getMessage());
		}
	}

}
