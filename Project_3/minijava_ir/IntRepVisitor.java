import visitor.GJDepthFirst;
import syntaxtree.*;

public class IntRepVisitor extends GJDepthFirst<String,SymbolTable> {
    private int currentTemp;
    private int currentLabel;
    private int tabs;
    private boolean rvalue;//becomes true in PE visitor
    private boolean meth_call;//to save class name in method call
    private String call_class;
    private String messSendArgs;

    IntRepVisitor(){
        currentTemp = 0;
        currentLabel = 0;
        tabs = 0;
        rvalue = false;
        meth_call = false;
        call_class = "";
        messSendArgs = "";
    }

    private String nextTemp(){
        String s = "%_" + currentTemp + "";
        currentTemp += 1;
        return s;
    }

    private void restartTemp(){ currentTemp = 0; }
    private void increaseTabs(){ tabs += 1; }
    private void decreaseTabs(){ if(tabs != 0)tabs -= 1; }//never go less than 0

    private String nextLabel(){
        String s = "label" + currentLabel;
        currentLabel += 1;
        return s;
    }

    private void emit(String s){
        for(int i=0;i<tabs;i++)
            System.out.print("\t");
        System.out.println(s);
    }

    private void printStartingCode(){
        emit("\n");
        emit("declare i8* @calloc(i32, i32)");
        emit("declare i32 @printf(i8*, ...)");
        emit("declare void @exit(i32)\n");
        emit("@_cint = constant [4 x i8] c\"%d\\0a\\00\"");
        emit("@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"");
        emit("define void @print_int(i32 %i) {");
        emit("\t%_str = bitcast [4 x i8]* @_cint to i8*");
        emit("\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)");
        emit("\tret void\n}\n");
        emit("define void @throw_oob() {");
        emit("\t%_str = bitcast [15 x i8]* @_cOOB to i8*");
        emit("\tcall i32 (i8*, ...) @printf(i8* %_str)");
        emit("\tcall void @exit(i32 1)");
        emit("\tret void\n}\n\n");
    }

   /**
    * f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    */
   public String visit(Goal n, SymbolTable st) throws Exception {
      st.create_vtable();
      st.print_vtable();//for every class print in file its v-table
      printStartingCode();
      n.f0.accept(this, st);
      n.f1.accept(this, st);
      n.f2.accept(this, st);
      return null;
   }

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
   public String visit(MainClass n, SymbolTable st) throws Exception {
      String myClass = n.f1.accept(this, st);//dont need main class name
      //n.f11.accept(this, st);//neither args
      emit("define i32 @main() {");
      increaseTabs();
      st.enterScope(myClass);
      st.enterFuncScope("main");
      n.f14.accept(this, st);
      n.f15.accept(this, st);
      emit("ret i32 0");
      decreaseTabs();
      emit("}");
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
   public String visit(ClassDeclaration n, SymbolTable st) throws Exception {
      String myClass = n.f1.accept(this, st);
      st.enterScope(myClass);//myClass exists no prob to enter
      //n.f3.accept(this, argu);//nothing to do here(only on new ClassName() create array with those names)
      n.f4.accept(this, st);//need class name for method declerations
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
   public String visit(ClassExtendsDeclaration n, SymbolTable st) throws Exception {
      String myClass = n.f1.accept(this, st);
      String myParent = n.f3.accept(this, st);
      st.enterScope(myClass);
      st.AddParent(myParent);//maybe useful
      //n.f5.accept(this, argu);//nothing again
      n.f6.accept(this, st);
      st.exitScope();
      return null;
   }

   /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
   public String visit(VarDeclaration n, SymbolTable st) throws Exception {
      String type = n.f0.accept(this, st);
      String id = n.f1.accept(this, st);

      String varIType = "i32*";
      if(!type.equals("int[]")) varIType = st.findI(type);

      emit("%"+id+" = alloca "+ varIType);//local var: %id = alloca i_

      return null;
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
   public String visit(MethodDeclaration n, SymbolTable st) throws Exception {
      restartTemp();//restart temp regs for every method
      String type = n.f1.accept(this, st);
      String func = n.f2.accept(this, st);
      st.enterFuncScope(func);
      String funcArgs = st.getFIArgs();//get func args in representation: "i8* this, i_ %.arg1,..."
      String clfu = st.getClassName() + "." + func;
      emit("define "+st.findI(type)+" @"+clfu+"("+funcArgs+") {");//define func
      increaseTabs();
      n.f4.accept(this, st);
      n.f7.accept(this, st);
      n.f8.accept(this, st);
      String expr = n.f10.accept(this, st);
      emit("ret "+expr);
      decreaseTabs();
      st.exitFuncScope();
      emit("}\n");
      return null;
   }

   /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
   public String visit(FormalParameter n, SymbolTable st) throws Exception {
      String type = n.f0.accept(this, st);
      String id = n.f1.accept(this, st);
      String itype = st.findI(type);
      emit("%"+id+" = alloca "+itype);
      emit("store "+itype+" %."+id+", "+itype+"* %"+id);
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
   public String visit(AssignmentStatement n, SymbolTable st) throws Exception {
      String id = n.f0.accept(this, st);
      String idType = st.lookup(id);//get type of id
      String idiType = "i32*";
      if(!idType.equals("int[]")) idiType = st.findI(idType);//get i_ type
      int ofs = st.whatAmI(id);//ret -1 if func variable, else it's offset in class

      //ofs = -1 ---> store value in %id
      //else     ---> load -> getelementptr -> bitcast -> store
      String expr = n.f2.accept(this, st);//expr --> "<type> <temp>"
      //String[] ss = expr.split("\\s+");
      //String type_expr = ss[0];
      //expr = ss[1];
/* goes on identifier if rvalue
      String t0 = nextTemp();//load expr even if it
      emit(t0+" = load "+type_expr+", "+type_expr+"* %"+expr);
*/
      if(ofs != -1){
         String t1 = nextTemp();
         emit(t1+" = getelementptr i8, i8* %this, i32 "+ofs);//from class_var get field
         String t2 = nextTemp();
         emit(t2+" = bitcast i8* "+t1+" to "+idiType+"*");
         emit("store "+expr+", "+idiType+"* "+t2);
      }
      else{
         emit("store "+expr+", "+idiType+"* %"+id);
      }
      emit("");

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
   public String visit(ArrayAssignmentStatement n, SymbolTable st) throws Exception {
      String id = n.f0.accept(this, st);//type=int[] , itype=i8*
      String ex1 = n.f2.accept(this, st);//ex1 --> "<type1> <temp1>"(type1 must be i32)
      String[] ss = ex1.split("\\s+");
      ex1 = ss[1];//has to be < array.length(else oob)

      int ofs = st.whatAmI(id);

      String len = nextTemp();
      String arr_t;
      if(ofs == -1){
         //String t00 = nextTemp();
         //emit(t00+" = bitcast i8** "+id+" to i32**");
         String t0 = nextTemp();
         emit(t0+" = load i32*, i32** %"+id);
         String t1 = nextTemp();
         emit(len+" = load i32, i32* "+t0);
         arr_t = t0;
      }
      else{
         String t0 = nextTemp();
         emit(t0+" = getelementptr i8, i8* %this, i32 "+ofs);
         String t1 = nextTemp();
         emit(t1+" = bitcast i8* "+t0+" to i32**");
         String t2 = nextTemp();
         emit(t2+" = load i32*, i32** "+t1);
         emit(len+" = load i32, i32* "+t2);
         arr_t = t2;
      }

      String tt0 = nextTemp();
      emit(tt0+" = icmp ult i32 "+ex1+", "+len);

      String oob1 = nextLabel();
      String oob2 = nextLabel();
      String oob3 = nextLabel();

      String tt2 = nextTemp();
      emit("br i1 "+tt0+" ,label %"+oob1+" ,label %"+oob2+"\n\n"+oob1+":");

      String tt3 = nextTemp();
      emit(tt3+" = add i32 1, "+ ex1);//to get index(0 is for len,so all indexes are "index+1")
      String tt4 = nextTemp();
      emit(tt4+" = getelementptr i32, i32* "+arr_t+", i32 "+tt3);

      String ex2 = n.f5.accept(this, st);//ex2 --> "<type2> <temp2>"(type2 must be i32)

      emit("store "+ex2+", i32* "+tt4);
      emit("br label %"+oob3+"\n\n"+oob2+":");

      emit("call void @throw_oob()");
      emit("br label %"+oob3+"\n\n"+oob3+":");

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
   public String visit(IfStatement n, SymbolTable st) throws Exception {
      String expr = n.f2.accept(this, st);//expr --> "i1 t_"
      String l1 = nextLabel();//expr->true
      String l2 = nextLabel();//expr->false
      String l3 = nextLabel();//end label
      emit("br "+expr+", label %"+l1+", label %"+l2+"\n\n"+l1+":");
      n.f4.accept(this, st);
      emit("br label %"+l3+"\n\n"+l2+":");//on true go to end(after print else_label)
      n.f6.accept(this, st);
      emit("br label %"+l3+"\n\n"+l3+":");
      return null;
   }


   /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
   public String visit(WhileStatement n, SymbolTable st) throws Exception {
      String l1 = nextLabel();//before check expr
      String l2 = nextLabel();//expr->true
      String l3 = nextLabel();//expr->false
      emit("br label %"+l1+"\n\n"+l1+":");
      String expr = n.f2.accept(this, st);
      emit("br "+expr+", label %"+l2+", label %"+l3+"\n\n"+l2+":");
      n.f4.accept(this, st);
      emit("br label %"+l1+"\n\n"+l3+":");
      return null;
   }

   /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
   public String visit(PrintStatement n, SymbolTable st) throws Exception {
      String expr = n.f2.accept(this, st);
      String[] ss = expr.split("\\s+");
      String type_expr = ss[0];
      expr = ss[1];
      if(type_expr.equals("i1")){//boolean to int
         String t0 = nextTemp();
         emit(t0+" = zext i1 "+expr+" to i32");
         emit("call void (i32) @print_int(i32 "+t0+")");
      }
      else//int
         emit("call void (i32) @print_int(i32 "+expr+")");
      return null;
   }



//~~~~~~~~~~~~~

   /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
   public String visit(AndExpression n, SymbolTable st) throws Exception {
      String l0 = nextLabel();
      String l1 = nextLabel();
      String l2 = nextLabel();
      String l3 = nextLabel();
      String cl1 = n.f0.accept(this, st);
      emit("br label %"+l0+"\n\n"+l0+":");
      String[] ss = cl1.split("\\s+");
      String ss1 = ss[1];
      emit("br "+cl1+", label %"+l1+", label %"+l3+"\n\n"+l1+":");
      String cl2 = n.f2.accept(this, st);
      emit("br label %"+l2+"\n\n"+l2+":");
      emit("br label %"+l3+"\n\n"+l3+":");
      ss = cl2.split("\\s+");
      String t0 = nextTemp();
      emit(t0+" = phi i1 ["+ss1+", %"+l0+"], ["+ss[1]+", %"+l2+"]");
      return "i1 "+t0;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
   public String visit(CompareExpression n, SymbolTable st) throws Exception {
      String pe1 = n.f0.accept(this, st);//pe1="typ1 temp1"
      String pe2 = n.f2.accept(this, st);
      String[] ss = pe1.split("\\s+");
      pe1 = ss[1];//keep only the name
      ss = pe2.split("\\s+");
      pe2 = ss[1];//keep only the name
      String t0 = nextTemp();
      emit(t0+" = icmp slt i32 "+pe1+", "+pe2);
      return "i1 "+t0;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
   public String visit(PlusExpression n, SymbolTable st) throws Exception {
      String pe1 = n.f0.accept(this, st);
      String pe2 = n.f2.accept(this, st);
      String[] ss = pe1.split("\\s+");
      pe1 = ss[1];//keep only the name
      ss = pe2.split("\\s+");
      pe2 = ss[1];//keep only the name
      String t0 = nextTemp();
      emit(t0+" = add i32 "+pe1+", "+pe2);
      return "i32 "+t0;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
   public String visit(MinusExpression n, SymbolTable st) throws Exception {
      String pe1 = n.f0.accept(this, st);
      String pe2 = n.f2.accept(this, st);
      String[] ss = pe1.split("\\s+");
      pe1 = ss[1];//keep only the name
      ss = pe2.split("\\s+");
      pe2 = ss[1];//keep only the name
      String t0 = nextTemp();
      emit(t0+" = sub i32 "+pe1+", "+pe2);
      return "i32 "+t0;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
   public String visit(TimesExpression n, SymbolTable st) throws Exception {
      String pe1 = n.f0.accept(this, st);
      String pe2 = n.f2.accept(this, st);
      String[] ss = pe1.split("\\s+");
      pe1 = ss[1];//keep only the name
      ss = pe2.split("\\s+");
      pe2 = ss[1];//keep only the name
      String t0 = nextTemp();
      emit(t0+" = mul i32 "+pe1+", "+pe2);
      return "i32 "+t0;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
   public String visit(ArrayLookup n, SymbolTable st) throws Exception {
      String pe1 = n.f0.accept(this, st);
      String pe2 = n.f2.accept(this, st);//
      String[] ss = pe1.split("\\s+");
      pe1 = ss[1];//check if func-var or class-var with ofs
      ss = pe2.split("\\s+");
      pe2 = ss[1];//add 1 bc 0 is for length

      String t0 = nextTemp();
      emit(t0+" = load i32, i32* "+pe1);//length to check for oob
      String oob1 = nextLabel();
      String oob2 = nextLabel();
      String oob3 = nextLabel();

      String t1 = nextTemp();
      emit(t1+" = icmp ult i32 "+pe2+", "+t0);//unsigned comparison also for neg indexes

      String t2 = nextTemp();
      emit("br i1 "+t1+" ,label %"+oob1+" ,label %"+oob2+"\n\n"+oob1+":");

      String t3 = nextTemp();
      emit(t3+" = add i32 1, "+ pe2);//to get index(0 is for len,so all indexes are "index+1")
      String t4 = nextTemp();
      emit(t4+" = getelementptr i32, i32* "+pe1+", i32 "+t3);

      String t5 = nextTemp();
      emit(t5+" = load i32, i32* "+t4);
      emit("br label %"+oob3+"\n\n"+oob2+":");

      emit("call void @throw_oob()");
      emit("br label %"+oob3+"\n\n"+oob3+":");

      return "i32 "+t5;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
   public String visit(ArrayLength n, SymbolTable st) throws Exception {
      String pe1 = n.f0.accept(this, st);
      String[] ss = pe1.split("\\s+");
      pe1 = ss[1];//check if func-var or class-var with ofs

//rvalue id-visitor made the hard work searching and getting i32* temp as pe1
      String t0 = nextTemp();
      emit(t0+" = load i32, i32* "+pe1);
      return "i32 "+t0;
   }
/*
      int ofs = st.whatAmI(pe1);

      if(ofs == -1){
         String t0 = nextTemp();
         emit(t0+" = load i32*, i32** "+pe1);
         String t1 = nextTemp();
         emit(t1+" = load i32, i32* "+t0);
         return "i32 "+t1;
      }
      else{
         String t0 = nextTemp();
         emit(t0+" = getelementptr i8, i8* this, i32 "+ofs);
         String t1 = nextTemp();
         emit(t1+" = bitcast i8* "+t0+" to i32**");
         String t2 = nextTemp();
         emit(t2+" = load i32*, i32** "+t1);
         String t3 = nextTemp();
         emit(t3+" = load i32, i32* "+t2);
      }
      return "i32 "+t3;
   }*/

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
   public String visit(MessageSend n, SymbolTable st) throws Exception {
      boolean flag = false;
      if(meth_call) flag = true;
      boolean prev_meth_call = meth_call;
      String prev_call_class = call_class;
      meth_call = true;
      String pe1 = n.f0.accept(this, st);//will load class var then return "i8* %_X"
      String curr_class = call_class;
      meth_call = false;
      String[] ss = pe1.split("\\s+");
      String cl_var = ss[1];
      String func = n.f2.accept(this, st);
      String funcType = st.lookupF2(curr_class,func);

      String funciType = "i32*";
      if(!funciType.equals("int[]")) funciType = st.findI(funcType);//get i_ type
      String F_I_TYPE = st.getCFtype(curr_class,func);
      int F_offset = st.getFoffset(curr_class,func)/8;
      String prev_messSendArgs = messSendArgs;
      messSendArgs = "";
      n.f4.accept(this, st);//save args in messSendArgs
      if(messSendArgs == "") messSendArgs = ")";
      String t0 = nextTemp();
      emit(t0+" = bitcast "+pe1+" to i8***");
      String t1 = nextTemp();
      emit(t1+" = load i8**, i8*** "+t0);
      String t2 = nextTemp();
      emit(t2+" = getelementptr i8*, i8** "+t1+", i32 "+F_offset);
      String t3 = nextTemp();
      emit(t3+" = load i8*, i8** "+t2);
      String t4 = nextTemp();
      emit(t4+" = bitcast i8* "+t3+" to "+F_I_TYPE);
      String t5 = nextTemp();
      emit(t5+" = call "+funciType+" "+t4+"("+pe1+messSendArgs);
      meth_call = prev_meth_call;
      messSendArgs = prev_messSendArgs;
      if(!flag)
         call_class = prev_call_class;
      else
         call_class = funcType;//should be class name :))
      return funciType+" "+t5;
   }

   /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
   public String visit(ExpressionList n, SymbolTable st) throws Exception {
      String ex = n.f0.accept(this, st);
      if(ex != null)
         messSendArgs += "," + ex;
      n.f1.accept(this, st);
      messSendArgs += ")";
      return null;
   }

   /**
    * f0 -> ( ExpressionTerm() )*
    */
   public String visit(ExpressionTail n, SymbolTable st) throws Exception {
      n.f0.accept(this, st);
      return null;
   }

   /**
    * f0 -> ","
    * f1 -> Expression()
    */
   public String visit(ExpressionTerm n, SymbolTable st) throws Exception {
      String ex = n.f1.accept(this, st);
      if(ex != null)
         messSendArgs += "," + ex;
      return null;
   }

//~~~~~~~~~~~~~


   /**
    * f0 -> NotExpression()
    *       | PrimaryExpression()
    */
   public String visit(Clause n, SymbolTable st) throws Exception {
      return n.f0.accept(this, st);
   }

   public String visit(PrimaryExpression n, SymbolTable st) throws Exception {
      rvalue = true;
      String ret = n.f0.accept(this, st);
      rvalue = false;
      return ret;
   }


   /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
   public String visit(ArrayAllocationExpression n, SymbolTable st) throws Exception {
      String ArraySZ = n.f3.accept(this, st);//a temp_reg with array size
      String[] ss = ArraySZ.split("\\s+");
      String t_sz = ss[1];
      //String t0 = nextTemp();
      //emit(t0+" = load i32, i32* "+ArraySZ);//done in expr-visitor
      String t1 = nextTemp();
      emit(t1+" = icmp slt i32 "+t_sz+", 0");
      String l1 = nextLabel();
      String l2 = nextLabel();
      emit("br i1 "+t1+", label %"+l1+", label %"+l2+"\n\n"+l1+":");
      //out of bounds here in l1(negative size)
      emit("call void @throw_oob()");
      emit("br label %"+l2+"\n\n"+l2+":");//before "label_:" needs a br
      String t2 = nextTemp();
      emit(t2+" = add i32 "+t_sz+", 1");//add 1 to array size to add as first array element the size
      String t3 = nextTemp();
      emit(t3+" = call i8* @calloc(i32 4, i32 "+t2+")");//allocate space for array
      String t4 = nextTemp();
      emit(t4+" = bitcast i8* "+t3+" to i32*");//bitcast to save the size
      emit("store i32 "+t_sz+", i32* "+t4);
      return "i32* " + t4;//temp reg that has allocated array in its real form()
   }

   /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
   public String visit(AllocationExpression n, SymbolTable st) throws Exception {
      rvalue = false;//we want back only id-name and handle the rest here
      String myClass = n.f1.accept(this, st);
      if(meth_call) call_class = myClass;//keep class name for method call
      int ofs = st.getMyIdCounter(myClass);
      String t0 = nextTemp();
      emit(t0+" = call i8* @calloc(i32 1, i32 "+ofs+")");//0-alloc space for class vars + v-table
      String t1 = nextTemp();
      emit(t1+" = bitcast i8* "+t0+" to i8***");
      String t2 = nextTemp();
      int fn = st.getVTFuncNum(myClass);//fn: number of functions in vtable
      emit(t2+" = getelementptr ["+fn+" x i8*], ["+fn+" x i8*]* @."+myClass+"_vtable, i32 0, i32 0");
      emit("store i8** "+t2+", i8*** "+t1);
      return "i8* " + t0;//temp reg that has allocated class var + v-table space
   }

   /**
    * f0 -> <INTEGER_LITERAL>
    */
   public String visit(IntegerLiteral n, SymbolTable st) throws Exception {
      return "i32 " + n.f0.toString();
   }

   /**
    * f0 -> "true"
    */
   public String visit(TrueLiteral n, SymbolTable st) throws Exception {
      return "i1 1";
   }

   /**
    * f0 -> "false"
    */
   public String visit(FalseLiteral n, SymbolTable st) throws Exception {
      return "i1 0";
   }

   /**
    * f0 -> "this"
    */
   public String visit(ThisExpression n, SymbolTable st) throws Exception {
      if(meth_call) call_class = st.getClassName();//save classname for method calls like this.method(..)
      return "i8* %this";
   }

   /**
    * f0 -> <IDENTIFIER>
    */
   public String visit(Identifier n, SymbolTable st) throws Exception{
      String id = n.f0.toString();
      if(!rvalue) return id;//if id-visitor is lvalue just return the name
      String idType = st.lookup(id);//get type of id

      if(meth_call) call_class = idType;//id class

      String idiType = "i32*";
      if(!idType.equals("int[]")) idiType = st.findI(idType);//get i_ type
      int ofs = st.whatAmI(id);
      String t0 = nextTemp();

      if(ofs == -1){//func-var to load
         emit(t0+" = load "+idiType+", "+idiType+"* %"+id);
         return idiType+" "+t0;
      }
      else{//class-val: getelementptr->bitcast->load
         String t1 = nextTemp();
         String t2 = nextTemp();
         emit(t0+" = getelementptr i8, i8* %this, i32 "+ofs);//from class_var get field
         emit(t1+" = bitcast i8* "+t0+" to "+idiType+"*");
         emit(t2+" = load "+idiType+", "+idiType+"* "+t1);
         return idiType+" "+t2;
      }
   }

   /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
   public String visit(BracketExpression n, SymbolTable st) throws Exception {
      return n.f1.accept(this, st);
   }

   /**
    * f0 -> "!"
    * f1 -> Clause()
    */
   public String visit(NotExpression n, SymbolTable st) throws Exception {
      String cl = n.f1.accept(this, st);
      String[] ss = cl.split("\\s+");
      cl = ss[1];
      String t0 = nextTemp();
      emit(t0+" = xor i1 "+cl+", 1");//!t0 == t0^1
      return "i1 "+t0;
   }

}
