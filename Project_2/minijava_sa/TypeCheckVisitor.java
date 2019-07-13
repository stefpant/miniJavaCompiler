import visitor.GJDepthFirst;
import syntaxtree.*;
import java.util.LinkedList;

//passing ST to visitors to lookup variables in scopes
//visitors return node types as strings
//and identifier names to lookup them

public class TypeCheckVisitor extends GJDepthFirst<String,SymbolTable> {

   /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> "public"
    * f4 -> "static"
    * f5 -> "void"
    * f6 -> "main"
    * f7 -> "("
    * f8 -> "String"
    * f9 -> "["
    * f10 -> "]"
    * f11 -> Identifier()
    * f12 -> ")"
    * f13 -> "{"
    * f14 -> ( VarDeclaration() )*
    * f15 -> ( Statement() )*
    * f16 -> "}"
    * f17 -> "}"
    */
   public String visit(MainClass n, SymbolTable st) throws Exception{
      String class_name = n.f1.accept(this, st);

      if(!(st.enterScope(class_name)))//should not happen(fill visitor already inserted main in st)
         throw new Exception("Invalid name "+class_name+"(class not found)!");

      if(!(st.enterFuncScope("main")))
         throw new Exception("Invalid func name in "+class_name+"(should not happen :-])!");

      String args = n.f11.accept(this, st);
      //n.f14.accept(this, argu);
      n.f15.accept(this, st);
      st.exitFuncScope();
      st.exitScope();
      return null;
   }

   /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
   public String visit(ClassDeclaration n, SymbolTable st) throws Exception{
      String class_name = n.f1.accept(this, st);
      if(!(st.enterScope(class_name)))//should not happen(fill visitor already inserted class in st)
         throw new Exception("Invalid name "+class_name+"(class not found)!");

      //n.f3.accept(this, argu);
      n.f4.accept(this, st);

      st.exitScope();
      return null;
   }


   /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "extends"
    * f3 -> Identifier()
    * f4 -> "{"
    * f5 -> ( VarDeclaration() )*
    * f6 -> ( MethodDeclaration() )*
    * f7 -> "}"
    */
   public String visit(ClassExtendsDeclaration n, SymbolTable st) throws Exception{
      String class_name = n.f1.accept(this, st);

      if(!(st.enterScope(class_name)))//should not happen(fill visitor already inserted class in st)
         throw new Exception("Invalid name "+class_name+"(class not found)!");

      //String parent = n.f3.accept(this, st);//parent name -> useless info
      //n.f5.accept(this, argu);
      n.f6.accept(this, st);

      st.exitScope();
      return null;
   }//actually same as ClassDeclaration visit method


   /**
    * f0 -> "public"
    * f1 -> Type()
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( FormalParameterList() )?
    * f5 -> ")"
    * f6 -> "{"
    * f7 -> ( VarDeclaration() )*
    * f8 -> ( Statement() )*
    * f9 -> "return"
    * f10 -> Expression()
    * f11 -> ";"
    * f12 -> "}"
    */
   public String visit(MethodDeclaration n, SymbolTable st) throws Exception{
      String ret_type = n.f1.accept(this, st);
      String func_name = n.f2.accept(this, st);
      if(!(st.enterFuncScope(func_name)))
         throw new Exception("Invalid funcion "+func_name+"(should not happen :-])!");
      //n.f4.accept(this, st);
      //n.f7.accept(this, st);
      n.f8.accept(this, st);

      String ret_expr = n.f10.accept(this, st);

      if(!( ret_type.equals(ret_expr) ))
         throw new Exception("Return value does not match the function type in "+func_name+" function!");
      st.exitFuncScope();
      return null;
   }

   /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
   public String visit(ArrayType n, SymbolTable st) throws Exception{
      return "int[]";
   }

   /**
    * f0 -> "boolean"
    */
   public String visit(BooleanType n, SymbolTable st) throws Exception{
      return "boolean";
   }

   /**
    * f0 -> "int"
    */
   public String visit(IntegerType n, SymbolTable st) throws Exception{
      return "int";
   }


   /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
   public String visit(AssignmentStatement n, SymbolTable st) throws Exception{
      String id = n.f0.accept(this, st);
      String type_id = st.lookup(id);
      if(type_id == null)//should not happen
         throw new Exception("Identifier "+id+" not found!");
      String expr = n.f2.accept(this, st);
      if(!( type_id.equals(expr) ))//if not same class
         if(!( st.isValidAssign(type_id,expr) ))//type_id could be a parent class of 'expr'
            throw new Exception("Can't assign "+expr+" in identifier "+id+"(Expecting "+type_id+")!");
      return null;
   }

   /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
   public String visit(ArrayAssignmentStatement n, SymbolTable st) throws Exception{
      String id = n.f0.accept(this, st);
      String type_id = st.lookup(id);
      if(type_id == null)//should not happen
         throw new Exception("Identifier "+id+" not found!");
      if(!( type_id.equals("int[]") ))
         throw new Exception("Identifier "+id+" should be an array of integers!");
      String e1 = n.f2.accept(this, st);
      if(!( e1.equals("int") ))
         throw new Exception("Expression type inside '[' ']' must be integer!");
      String e2 = n.f5.accept(this, st);
      if(!( e2.equals("int") ))
         throw new Exception("Expression type assigned to int array must be integer!");
      return null;
   }


   /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
   public String visit(IfStatement n, SymbolTable st) throws Exception{
      String if_expr = n.f2.accept(this, st);
      if(!( if_expr.equals("boolean") ))
         throw new Exception("Expression inside if statement must be boolean type!");
      n.f4.accept(this, st);
      n.f6.accept(this, st);
      return null;
   }

   /*
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
   public String visit(WhileStatement n, SymbolTable st) throws Exception{
      String while_expr = n.f2.accept(this, st);
      if(!( while_expr.equals("boolean") ))
         throw new Exception("Expression inside while statement must be boolean type!");
      n.f4.accept(this, st);
      return null;
   }

   /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
   public String visit(PrintStatement n, SymbolTable st) throws Exception{
      String print_expression = n.f2.accept(this, st);
      if(!( print_expression.equals("int") ) && !( print_expression.equals("boolean") ))
         throw new Exception("System.out.println prints only integers or boolean!");
      return null;
   }

/******
PrimaryExpression could be:"int","boolean","this"(-->type of class we are currently) and identifier
(Clause is a PrimaryExpression too)
******/

   /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
   public String visit(AndExpression n, SymbolTable st) throws Exception{
      String c1 = n.f0.accept(this, st);
      String type_c1 = st.lookup(c1);//lookup ret null only if it got id and didnt found it 
      if(type_c1 == null)
         throw new Exception("Undefined identifier "+c1+"!");
      else if(!( type_c1.equals("boolean") ))
         throw new Exception("AND('&&') operation applies to boolean!");
      String c2 = n.f2.accept(this, st);
      String type_c2 = st.lookup(c2);//lookup ret null only if it got id and didnt found it
      if(type_c2 == null)
         throw new Exception("Undefined identifier "+c2+"!");
      else if(!( type_c2.equals("boolean") ))
         throw new Exception("AND('&&') operation applies to boolean!");
      return "boolean";
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
   public String visit(CompareExpression n, SymbolTable st) throws Exception{
      String pe1 = n.f0.accept(this, st);
      String type_pe1 = st.lookup(pe1);//lookup ret null only if it got id and didnt found it 
      if(type_pe1 == null)
         throw new Exception("Undefined identifier "+pe1+"!");
      else if(!( type_pe1.equals("int") ))
         throw new Exception("COMPARE('<') operation applies to integers!");
      String pe2 = n.f2.accept(this, st);
      String type_pe2 = st.lookup(pe2);//lookup ret null only if it got id and didnt found it
      if(type_pe2 == null)
         throw new Exception("Undefined identifier "+pe2+"!");
      else if(!( type_pe2.equals("int") ))
         throw new Exception("COMPARE('<') operation applies to integers!");
      return "boolean";
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
   public String visit(PlusExpression n, SymbolTable st) throws Exception{
      String pe1 = n.f0.accept(this, st);
      String type_pe1 = st.lookup(pe1);//lookup ret null only if it got id and didnt found it 
      if(type_pe1 == null)
         throw new Exception("Undefined identifier "+pe1+"!");
      else if(!( type_pe1.equals("int") ))
         throw new Exception("PLUS('+') operation applies to integers!");
      String pe2 = n.f2.accept(this, st);
      String type_pe2 = st.lookup(pe2);//lookup ret null only if it got id and didnt found it
      if(type_pe2 == null)
         throw new Exception("Undefined identifier "+pe2+"!");
      else if(!( type_pe2.equals("int") ))
         throw new Exception("PLUS('+') operation applies to integers!");
      return "int";
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
   public String visit(MinusExpression n, SymbolTable st) throws Exception{
      String pe1 = n.f0.accept(this, st);
      String type_pe1 = st.lookup(pe1);//lookup ret null only if it got id and didnt found it 
      if(type_pe1 == null)
         throw new Exception("Undefined identifier "+pe1+"!");
      else if(!( type_pe1.equals("int") ))
         throw new Exception("MINUS('-') operation applies to integers!");
      String pe2 = n.f2.accept(this, st);
      String type_pe2 = st.lookup(pe2);//lookup ret null only if it got id and didnt found it
      if(type_pe2 == null)
         throw new Exception("Undefined identifier "+pe2+"!");
      else if(!( type_pe2.equals("int") ))
         throw new Exception("MINUS('-') operation applies to integers!");
      return "int";
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
   public String visit(TimesExpression n, SymbolTable st) throws Exception{
      String pe1 = n.f0.accept(this, st);
      String type_pe1 = st.lookup(pe1);//lookup ret null only if it got id and didnt found it 
      if(type_pe1 == null)
         throw new Exception("Undefined identifier "+pe1+"!");
      else if(!( type_pe1.equals("int") ))
         throw new Exception("TIMES('*') operation applies to integers!");
      String pe2 = n.f2.accept(this, st);
      String type_pe2 = st.lookup(pe2);//lookup ret null only if it got id and didnt found it
      if(type_pe2 == null)
         throw new Exception("Undefined identifier "+pe2+"!");
      else if(!( type_pe2.equals("int") ))
         throw new Exception("TIMES('*') operation applies to integers!");
      return "int";
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
   public String visit(ArrayLookup n, SymbolTable st) throws Exception{
      String pe1 = n.f0.accept(this, st);//should be id-->int[]
      String type_pe1 = st.lookup(pe1);//lookup ret null only if it got id and didnt found it
      if(type_pe1 == null)
         throw new Exception("Undefined identifier "+pe1+"!");
      else if(!( type_pe1.equals("int[]") ))
         throw new Exception("Tried to lookup in not array variable!");
      String pe2 = n.f2.accept(this, st);//should be type of "int"(array pos)
      String type_pe2 = st.lookup(pe2);//lookup ret null only if it got id and didnt found it
      if(type_pe2 == null)
         throw new Exception("Undefined identifier "+pe2+"!");
      else if(!( type_pe2.equals("int") ))
         throw new Exception("Expected int as array position(found "+type_pe2+")!");
      return "int";//value in array's pos is integer
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
   public String visit(ArrayLength n, SymbolTable st) throws Exception{
      String pe1 = n.f0.accept(this, st);//should be id-->int[]
      String type_pe1 = st.lookup(pe1);//lookup ret null only if it got id and didnt found it
      if(type_pe1 == null)
         throw new Exception("Undefined identifier "+pe1+"!");
      else if(!( type_pe1.equals("int[]") ))
         throw new Exception("Only array variables support \"length\" attribute!");
      return "int";//returns length of array-->int
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
   public String visit(MessageSend n, SymbolTable st) throws Exception{
      String id = n.f0.accept(this, st);//should be id--> class type
      String type_id = st.lookup(id);//lookup ret null only if it got id and didnt found it
      if(type_id == null)
         throw new Exception("Undefined identifier "+id+"!");
      else if(type_id.equals("int") || type_id.equals("int[]") || type_id.equals("boolean"))
         throw new Exception(id+" expected to be identifier of class type!");
      String func = n.f2.accept(this, st);//should be a function called like id.func(...)
      LinkedList<String> init_del = st.InitMessageSendList();//init list of args to lookup func
      n.f4.accept(this, st);//will insert arg_types in list
      String f_type = st.lookupF(type_id,func);//ret func type,also checking number and type of args
      if(f_type == null)
         throw new Exception("Method \""+func+"\" not found!");
      st.DeleteMessageSendList(init_del);//init list of args to lookup func
      return f_type;
   }

   /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
   public String visit(ExpressionList n, SymbolTable st) throws Exception{
      String expr = n.f0.accept(this, st);
      String type_expr = st.lookup(expr);
      st.InsertMessageSendList(type_expr);//find expr type and then insert it in arg_type list
      n.f1.accept(this, st);
      return null;
   }

   /**
    * f0 -> ","
    * f1 -> Expression()
    */
   public String visit(ExpressionTerm n, SymbolTable st) throws Exception{
      String expr = n.f1.accept(this, st);
      st.InsertMessageSendList(st.lookup(expr));//find expr type and then insert it in arg_type list
      return null;
   }

   /**
    * f0 -> NotExpression()
    *       | PrimaryExpression()
    */
   public String visit(Clause n, SymbolTable st) throws Exception{
      String pe = n.f0.accept(this, st);
      String type_pe = st.lookup(pe);
      if(type_pe == null)
         throw new Exception("Undefined identifier "+pe+"!");
      return type_pe;
   }

   /**
    * f0 -> <INTEGER_LITERAL>
    */
   public String visit(IntegerLiteral n, SymbolTable st) throws Exception{
      return "int";
   }

   /**
    * f0 -> "true"
    */
   public String visit(TrueLiteral n, SymbolTable st) throws Exception{
      return "boolean";
   }

   /**
    * f0 -> "false"
    */
   public String visit(FalseLiteral n, SymbolTable st) throws Exception{
      return "boolean";
   }

   /**
    * f0 -> <IDENTIFIER>
    */
   public String visit(Identifier n, SymbolTable st) throws Exception{
      return n.f0.toString();
   }

   /**
    * f0 -> "this"
    */
   public String visit(ThisExpression n, SymbolTable st) throws Exception{
      return "this";
   }

   /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
   public String visit(ArrayAllocationExpression n, SymbolTable st) throws Exception{
      String expr = n.f3.accept(this, st);
      String type_expr = st.lookup(expr);//lookup ret null only if it got id and didnt found it
      if(type_expr == null)
         throw new Exception("Undefined identifier "+expr+"!");
      else if(!( type_expr.equals("int") ))
         throw new Exception("Expected integer value to allocate an array!");
      return "int[]";
   }

   /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
   public String visit(AllocationExpression n, SymbolTable st) throws Exception{
      return n.f1.accept(this, st);//identifier here should be a class name(like A a = new A();)
   }

   /**
    * f0 -> "!"
    * f1 -> Clause()
    */
   public String visit(NotExpression n, SymbolTable st) throws Exception{
      String c = n.f1.accept(this, st);
      String type_c = st.lookup(c);//lookup ret null only if it got id and didnt found it
      if(type_c == null)
         throw new Exception("Undefined identifier "+c+"!");
      else if(!( type_c.equals("boolean") ))
         throw new Exception("NOT('!') operation applies to boolean!");
      return "boolean";
   }

   /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
   public String visit(BracketExpression n, SymbolTable st) throws Exception{
      String expr = n.f1.accept(this, st);
      String type_expr = st.lookup(expr);//lookup ret null only if it got id and didnt found it
      if(type_expr == null)
         throw new Exception("Undefined identifier "+expr+"!");
      return type_expr;
   }

}
