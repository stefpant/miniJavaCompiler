import visitor.GJDepthFirst;
import syntaxtree.*;

public class MyFillSTVisitor extends GJDepthFirst<String,SymbolTable> {

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
      String _ret=null;
      String mainClassName = n.f1.accept(this, st);
      //let's enter new scope
      if(!(st.enterNewScope(mainClassName))){//error main class name exists
         //System.out.println("Invalid name "+mainClassName+"(class already exists)!");
         //System.exit(-1);//
         throw new Exception("Invalid name "+mainClassName+"(class already exists)!");
      }
      if(!(st.enterFunctionMode("main"))){
         //System.out.println("Invalid name 'main'(should not happen :-])!");
         //System.exit(-1);//
         throw new Exception("Invalid function name in "+mainClassName+"(should not happen :-])!");
      }
      st.Insert_Main_FuncInfo_Type("void");
      String mainArg = n.f11.accept(this, st);
      if(!(st.Insert_Main_FuncInfo_Arg(mainArg, "String[]"))){
         //System.out.println("Identifier "+mainArg+" already defined!");
         //System.exit(-1);//
         throw new Exception("Identifier "+mainArg+" already defined!");
      }
      n.f14.accept(this, st);
      n.f15.accept(this, st);
      st.exitFunctionMode();//insert main function in map
      st.exitNewScope();//instert class mainClassName in map
      return _ret;
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
      String _ret=null;
      String className = n.f1.accept(this, st);
      if(!(st.enterNewScope(className))){//error main class name exists
         //System.out.println("Invalid name "+className+"(class already exists)!");
         //System.exit(-1);//
         throw new Exception("Invalid name "+className+"(class already exists)!");
      }
      st.InitClassOffsets(null);
      n.f3.accept(this, st);//in var_decl insert (id,types) in IdNamesMap
      n.f4.accept(this, st);//methodDecl:(enter fm,init,exit fm )*

      st.InsertOffsetMap();

      st.exitNewScope();
      return _ret;
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
      String _ret=null;
      String className = n.f1.accept(this, st);
      if(!(st.enterNewScope(className))){//error main class name exists
         //System.out.println("Invalid name "+className+"(class already exists)!");
         //System.exit(-1);//
         throw new Exception("Invalid name "+className+"(class already exists)!");
      }
      String parent = n.f3.accept(this, st);
      if(!(st.AddParent(parent))){
         //System.out.println("Class "+parent+" must be defined first!");
         //System.exit(-1);//
         throw new Exception("Class "+parent+" must be defined first!");
      }
      st.InitClassOffsets(parent);
      n.f5.accept(this, st);
      n.f6.accept(this, st);

      st.InsertOffsetMap();
      st.InitFuctionCheck();
      //check methods for override...
      st.exitNewScope();
      //first exit scopes then check for override
      st.functionCheck(className);
      return _ret;
   }

   /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
   public String visit(VarDeclaration n, SymbolTable st) throws Exception{
      String _ret=null;
      String type = n.f0.accept(this, st);
      String id = n.f1.accept(this, st);
      if(st.in_function_mode()){//add (id,type) class's function's id map
         if(!(st.Insert_FuncInfo_Id(id,type))){
            //System.out.println("Variable name: '"+id+"' already in use in function "+ st.getFunctionName() +"!");
            //System.exit(-1);//
            throw new Exception("Variable name: '"+id+"' already in use in function "+ st.getFunctionName() +"!");
         }
      }
      else{//else in class's id map
         if(!(st.Insert_IdNamesMap(id,type))){
            //System.out.println("Variable name: '"+id+"' already in use in class "+ st.getClassName() +"!");
            //System.exit(-1);//
            throw new Exception("Variable name: '"+id+"' already in use in class "+ st.getClassName() +"!");
         }
         st.InsertIdToOffset(id,type);
      }
      return _ret;
   }


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
      String _ret=null;
      String Ftype = n.f1.accept(this, st);
      String Fname = n.f2.accept(this, st);
      if(!(st.enterFunctionMode(Fname))){
         //System.out.println("Function "+Fname+" already defined!");
         //System.exit(-1);//
         throw new Exception("Invalid name "+Fname+"(function already defined :-])!");
      }
      st.Insert_FuncInfo_Type(Ftype);
      st.InsertFuncToOffset(Fname);
      n.f4.accept(this, st);
      n.f7.accept(this, st);
      n.f8.accept(this, st);
      n.f10.accept(this, st);
      st.exitFunctionMode();
      return _ret;
   }


   /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
   public String visit(FormalParameter n, SymbolTable st) throws Exception{
      String _ret=null;
      String type = n.f0.accept(this, st);
      String id = n.f1.accept(this, st);
      if(!(st.Insert_FuncInfo_Arg(id,type))){
         //System.out.println("Argument '"+id+"' in function "+ st.getFunctionName() +" already in use!");
         //System.exit(-1);//
         throw new Exception("Argument '"+id+"' in function "+ st.getFunctionName() +" already in use!");
      }
      return _ret;
   }

   /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
   public String visit(ArrayType n, SymbolTable st) throws Exception{
//      n.f0.accept(this, argu);
//      n.f1.accept(this, argu);
//      n.f2.accept(this, argu);
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
    * f0 -> <IDENTIFIER>
    */
   public String visit(Identifier n, SymbolTable st) throws Exception{
      return n.f0.toString();
   }

}
