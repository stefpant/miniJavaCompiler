import java.io.InputStream;
import java.io.IOException;

class Parser{
	private int lookahead;//la
	private InputStream m;

	public Parser(InputStream m) throws IOException{
		this.m = m;
		while((lookahead = m.read()) == ' ') continue;
	}

	public int parse() throws IOException, ParseError{
		return Expr();
	}

	private void consume(int symbol) throws IOException, ParseError{
		if(lookahead != symbol)
			throw new ParseError();
		while((lookahead = m.read()) == ' ') continue;//ignore white space
	}

	private int Expr() throws IOException, ParseError{
		if((lookahead < '0' || lookahead > '9') && (lookahead != '('))//if la not a number or '('
			throw new ParseError();
		return Expr1(Term());//evaluates term as t,then in expr1 computes: (new_term_evaluation)^t
	}

	private int Expr1(int eval_term) throws IOException, ParseError{
		if(lookahead == '^'){
			consume('^');
			return Expr1(eval_term ^ Term());
		}
		if(lookahead == -1 || lookahead == 10 || lookahead == ')')//EOF is -1 , new line-->10
			return eval_term;

		//here for parse error if found number,& or '(' or whatever
		throw new ParseError();
	}

	private int Term() throws IOException, ParseError{
		if((lookahead < '0' || lookahead > '9') && (lookahead != '('))//if la not a number or '('
			throw new ParseError();
		return Term1(Factor());//same as Expr()
	}

	private int Term1(int eval_factor) throws IOException, ParseError{
		if(lookahead == '&'){
			consume('&');
			return Term1(eval_factor & Factor());
		}
		if(lookahead == -1 || lookahead == 10 || lookahead == ')' || lookahead == '^')
			return eval_factor;
		//if found something else then parse error
		throw new ParseError();
	}

	private int Factor() throws IOException, ParseError{
		if(lookahead == '('){// --> ( expr )
			consume(lookahead);
			int evaluate = Expr();
			consume(')');//lookahead here has to be ')'
			return evaluate;//then return result in parenthesis
		}
		if(lookahead < '0' || lookahead > '9')//error if not a number
			throw new ParseError();
		int current_num = lookahead - '0';
		consume(lookahead);
		return current_num;
	}
}
