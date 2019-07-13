import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;

public class SymbolTable {
    //These 3 Maps contain all useful symbol_table information
    private Map<String, String> ExtendsMap;//Map<A,B> when class A extends B(B=parent)
    private Map<String, Map<String, String>> IdClassScopeMap;//Map<className,Map<Identifier, Id_type>>
    private Map<String, Map<String, FuncInfo>> FuncClassScopeMap;//Map<className,Map<FuncName,info>>

    //class info when we enter its scope
    private String className;
    private Map<String, String> IdNamesMap;//after leaving scope->insert(className,IdNamesMap) in IdClassScopeMap
    private String funcName;
    private FuncInfo finfo;
    private Map<String, FuncInfo> FuncNamesMap;//after leaving scope->insert(className,FuncNamesMap) in FuncClassScopeMap

    //class Names could be referred as types before definition,so we check them
    private Set<String> referencedTypes;//after symbol table initialization

    //to check overriding
    private FunctionCheck myfc;

    //save offsets for every class
    private Map<String, offsetClass> MapOffsets;
    private offsetClass myOffsets;

    //save arg types to lookup function
    private LinkedList<String> fuArgTypes;

    SymbolTable(){
        ExtendsMap = new HashMap<String, String>();
        IdClassScopeMap = new HashMap<String, Map<String, String>>();
        FuncClassScopeMap = new HashMap<String, Map<String, FuncInfo>>();
        referencedTypes = new HashSet<String>();
        referencedTypes.add("int[]");//represent int array as "int[]" 
        referencedTypes.add("int");
        referencedTypes.add("boolean");
        MapOffsets = new LinkedHashMap<String, offsetClass>();
        myfc = null;
        className = null;
        IdNamesMap = null;
        funcName = null;
        finfo = null;
        FuncNamesMap = null;
        fuArgTypes = null;
    }

//---------------------- used only from fill visitor to create scopes ------------------------------
//create scope,then enter!
//className must not exists!
    public boolean enterNewScope(String className){
        if(this.className != null)
            return false;//already in a scope
        if(this.IdClassScopeMap.containsKey(className))
            return false;
        this.className = className;
        this.IdNamesMap = new HashMap<String, String>();
        this.FuncNamesMap = new HashMap<String, FuncInfo>();
        return true;
    }

//insert class variables and functions in maps,then exit
    public void exitNewScope(){
        if(className == null) return;//not in scope
        if(funcName != null && finfo != null){//if function not in Map
            FuncNamesMap.put(funcName,finfo);//already checked funcName is new in scope
            funcName = null;
            finfo = null;
        }
        IdClassScopeMap.put(className, IdNamesMap);
        FuncClassScopeMap.put(className, FuncNamesMap);
        IdNamesMap = null;
        FuncNamesMap = null;
        className = null;
    }
//--------------------------------------------------------------------------------------------------

//------------------ Mainly used from typecheck visitor( enter() - exit() ) ------------------------
    public boolean enterScope(String className){
        if(this.className != null)
            return false;//already in a scope
        if(!(this.IdClassScopeMap.containsKey(className)))
            return false;//false if class doesn't exist 
        this.className = className;
        this.IdNamesMap = IdClassScopeMap.get(className);
        this.FuncNamesMap = FuncClassScopeMap.get(className);
        return true;
    }

    public void exitScope(){
        IdNamesMap = null;
        FuncNamesMap = null;
        className = null;
        funcName = null;//these 2 should
        finfo = null;//already be null!
    }


//--------------------------------------------------------------------------------------------------

    public boolean AddParent(String parent){
        if(!(this.IdClassScopeMap.containsKey(parent)))
            return false;//parent must exist!
        //this avoids cyclic inheritance
        ExtendsMap.put(className,parent);
        return true;
    }

    public String getParent(){
        if(className == null) return null;
        return ExtendsMap.get(className);//returns null if className does not have a parent :'(
    }

    public boolean Insert_IdNamesMap(String id, String type){
        if(className == null) return false;//not in scope(code error:should not happen)
        if(IdNamesMap.containsKey(id)) return false;//(program error:var id already defined)
        referencedTypes.add(type);//whatever the type just add it to set
        IdNamesMap.put(id,type);
        return true;
    }

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ function_scope functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public boolean in_function_mode() {return (funcName != null);}

    public boolean enterFunctionMode(String fname){
        if(funcName != null) return false;//should not happen
        if(FuncNamesMap.containsKey(fname)) return false;//function fname already defined
        funcName = fname;
        return true;
    }

    public void exitFunctionMode(){
        if(funcName == null) return;
        FuncNamesMap.put(funcName,finfo);
        funcName = null;
        finfo = null;
    }

    public boolean enterFuncScope(String name){
        if(funcName != null) return false;//should not happen
        if(!( FuncNamesMap.containsKey(name) )) return false;//function fname not found
        funcName = name;
        return true;
    }

    public void exitFuncScope(){
        funcName = null;
        finfo = null;
    }
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public void Insert_Main_FuncInfo_Type(String type){
        finfo = new FuncInfo(type);
    }

    public void Insert_FuncInfo_Type(String type){
        referencedTypes.add(type);
        finfo = new FuncInfo(type);
    }

    public boolean Insert_Main_FuncInfo_Arg(String id, String type){
        finfo.insert_Arg(type);//void
        return finfo.insert_id(id,type);//boolean:check if id exists or not
    }

    public boolean Insert_FuncInfo_Arg(String id, String type){
        referencedTypes.add(type);
        finfo.insert_Arg(type);//void
        return finfo.insert_id(id,type);//boolean:check if id exists or not
    }

    public boolean Insert_FuncInfo_Id(String id, String type){
        referencedTypes.add(type);
        return finfo.insert_id(id,type);//boolean:check if id exists or not
    }


    //return value here could be "null"
    public String getFunctionName() {return this.funcName;}
    public String getClassName() {return this.className;}

//----------------------------------- Just for testing ---------------------------------------------
    public void print(){
        System.out.println("Extends Map:");
        for(String key : ExtendsMap.keySet())
            System.out.println("\tclass "+key+" extends "+ExtendsMap.get(key));
        System.out.println("Class Info:");
        for(String key : IdClassScopeMap.keySet()){
            System.out.println("\tclass:"+key);
            this.enterScope(key);
            for(String key2 : IdNamesMap.keySet())
                System.out.println("\t\t"+key2+"-->"+IdNamesMap.get(key2));
            for(String key2 : FuncNamesMap.keySet()){
                System.out.println("\t\tFunction:"+key2+"-->"+FuncNamesMap.get(key2).getType());
                FuncNamesMap.get(key2).print();
            }
            this.exitScope();
        }
        //System.out.println(referencedTypes);
    }
//--------------------------------------------------------------------------------------------------

//-------------------------------------- Override Checking -----------------------------------------
    public void InitFuctionCheck(){
        myfc = null;
        if(this.className == null) return;
        myfc = new FunctionCheck(this.className);
        for(String key2 : FuncNamesMap.keySet()){
            LinkedList<String> info = new LinkedList<String>();
            FuncNamesMap.get(key2).info_toList(info);
            info.addFirst(key2);//function name
            myfc.fc_info_add(info);
        }
    }

    //must be outside scopes
    //get functions class
    //then check all parents if class appears here
    //if found-> check types and remove from List(return false if found dif types)
    //continue to parent if it has one else return true
    public void functionCheck(String myClass)throws Exception{
        if(myfc == null) return ;//should not happen
        String curClass = myClass;
        while(true){
            if(!(ExtendsMap.containsKey(curClass))) return ;//no parent
            curClass = ExtendsMap.get(curClass);//change curClass with parent
            this.enterScope(curClass);
            for(List<String> func_key : myfc.get_fc_info()){
                if(FuncNamesMap.containsKey(func_key.get(0))){//func_key[0]==function name
                    finfo = FuncNamesMap.get(func_key.get(0));
                    if(!(finfo.checkSimilarity(func_key)))//if not similar types and arguments of overriden method
                        throw new Exception("Error overriding Method "+func_key.get(0)+" in Class "+myClass);
                    //myfc.removeValue(func_key);
                }
            }
            this.exitScope();
        }
    }
//--------------------------------------------------------------------------------------------------

//-------------------------- Checking referenced undefined classes ---------------------------------
    //Could return s or null to let Visitor print error message
    public void checkReferredTypes() throws Exception{
        referencedTypes.remove("int[]");
        referencedTypes.remove("boolean");
        referencedTypes.remove("int");
        for(String s : referencedTypes){
            if(!(IdClassScopeMap.containsKey(s))){
                //System.out.println("Type name "+s+" referenced but not defined!");
                //System.exit(-1);
                throw new Exception("Type name "+s+" referenced but not defined!");
            }
        }
    }
//--------------------------------------------------------------------------------------------------

//only used from fill visitor
//------------------------------------------ Offsets -----------------------------------------------
    
    public void InitClassOffsets(String class_parent){//parent could be null
        if(class_parent == null)
            myOffsets = new offsetClass(null);
        else
            myOffsets = new offsetClass(MapOffsets.get(class_parent));
    }

    //when we exit new scope insert in map
    public void InsertOffsetMap(){
        MapOffsets.put(className, myOffsets);
    }

    public void InsertIdToOffset(String name, String type){
        myOffsets.insertId(name,type);
    }

    public void InsertFuncToOffset(String name){
        int found = -1;
        String parentN,classN;
        classN = className;
        while(true){
            parentN = ExtendsMap.get(classN);
            if(parentN == null) break;
            Map<String, FuncInfo> temp = FuncClassScopeMap.get(parentN);
            if(temp.containsKey(name)){
                found = MapOffsets.get(parentN).getFuncOffset(name);
                break;
            }
            classN = parentN;
        }
        myOffsets.insertFunc(name,found);
    }

    public void printOffsets(){
        for(String key : MapOffsets.keySet()){
            offsetClass temp_class_offsets = MapOffsets.get(key);
//            if(temp_class_offsets.isEmpty()) continue;//1
            System.out.println("----------- Class:"+key+" -----------");
            temp_class_offsets.print_me(key);
        }
    }
//--------------------------------------------------------------------------------------------------

//---------------------------------------------   lookup   -----------------------------------------
//if function with args is called those 3 functions bellow manages the info we need to lookup func
    public LinkedList<String> InitMessageSendList(){
        LinkedList<String> ret_cur = fuArgTypes;//if it wasn't null when i del this list i'll init it
        fuArgTypes = new LinkedList<String>();//with ret_cur and not null
        return ret_cur;
    }

    public void InsertMessageSendList(String at){
        fuArgTypes.add(at);
    }

    public void DeleteMessageSendList(LinkedList<String> init_list){
        fuArgTypes = init_list;
    }

//id may not be an identifier(could be "int","boolean","this","int[]" or a class Name)
    public String lookup(String id){
        String cur_class_name = this.getClassName();
        String cur_func_name = this.getFunctionName();
        if(cur_class_name == null) return null;
        if(id.equals("int") || id.equals("int[]") || id.equals("boolean")) return id;
        if(id.equals("this")) return cur_class_name;
        if(this.IdClassScopeMap.containsKey(id)) return id;//class name
        //else id is identifier,then we search (1)func vars,(2)class vars and (3)parent class vars
        finfo = FuncNamesMap.get(cur_func_name);
        if(finfo != null){
            String id_type = finfo.getIdType(id);
            if(id_type != null) return id_type;//var id found in func vars
        }
        this.exitFuncScope();
        this.exitScope();
        String id_type = null,myClass = cur_class_name;// = IdNamesMap.get(id)
        while(myClass != null){
            this.enterScope(myClass);
            id_type = IdNamesMap.get(id);
            if(id_type != null) {this.exitScope(); break;}
            myClass = this.getParent();
            this.exitScope();
        }
        this.enterScope(cur_class_name);
        this.enterFuncScope(cur_func_name);
        return id_type;//may be null
    }

    //in class clName or parent class check for fuName function
    //then check number and type of args
    //if ok return fuName type :-)
    public String lookupF(String clName, String fuName) throws Exception{
        String cur_class_name = this.getClassName();//save current class and function
        String cur_func_name = this.getFunctionName();//to return here after func lookup
        this.exitFuncScope();
        this.exitScope();
        boolean flag=false;
        String fuName_type = null,myClass = clName;
        while(myClass != null){
            if(!enterScope(myClass)){flag=true; break;}//try to enter scope of myClass
            finfo = FuncNamesMap.get(fuName);//try to found fuName in myClass,null if not found
            if(finfo != null){//fuName found!!
                fuName_type = finfo.getType();
                LinkedList<String> temp = new LinkedList<String>(fuArgTypes);
                temp.addFirst(fuName_type);
                temp.addFirst(fuName);//then fuArgTypes==[fname,ftype,argtype1,...argtypeN]
                if(!(finfo.checkPolymorphicSimilarity(temp,ExtendsMap)))//if not similar arguments --> error
                    throw new Exception("Found different type or number of args for function "+fuName);
            }
            myClass = this.getParent();
            this.exitScope();
        }
        if(flag)
            throw new Exception("Didn't found function "+fuName+" while searching in class "+myClass+"(class not found)!");
        this.enterScope(cur_class_name);
        this.enterFuncScope(cur_func_name);
        return fuName_type;//may be null
    }

    public boolean isValidAssign(String parent,String child){
        String curCl = ExtendsMap.get(child);
        while(curCl != null){
            if(parent.equals(curCl)) return true;
            curCl = ExtendsMap.get(curCl);
        }
        return false;
    }

}


class FuncInfo {
    private String Type;
    private List<String> Arg_Types;
    private Map<String, String> Ids;//Map<Identifier, Type>(also Ids contains the func arguments)

    public FuncInfo(String type){
        this.Type = type;
        this.Arg_Types = new LinkedList<String>();
        this.Ids = new HashMap<String, String>();
    }

    public void insert_Arg(String type){
        Arg_Types.add(type);
    }

    public boolean insert_id(String id, String type){
        if(Ids.containsKey(id)) return false;//id already exists
        Ids.put(id,type);
        return true;
    }

    public String getType() {return this.Type;}

    public String getIdType(String id){
        return Ids.get(id);//return null if id not found there
    }

    public void print(){
        for(String id : Ids.keySet())
            System.out.println("\t\t\t"+id+"-->"+Ids.get(id));
    }

    public void info_toList(LinkedList<String> info){
        info.add(this.Type);
        for(String s : Arg_Types)
            info.add(s);
    }

    public boolean checkSimilarity(List<String> func){//func==[fname,ftype,argtype1,...,argtypeN]
        if(!((this.Type).equals(func.get(1)))) return false;
        if(Arg_Types.size() != func.size() - 2) return false;//dif number of arguments
        int counter = 2;
        for(String arg : Arg_Types){
            if(!(arg.equals(func.get(counter)))) return false;
            counter++;
        }
        return true;
    }

//same as checkSimilarity but 
    public boolean checkPolymorphicSimilarity(List<String> func,Map<String, String> ExtendsMap){//func==[fname,ftype,argtype1,...,argtypeN]
        if(!((this.Type).equals(func.get(1)))) return false;
        if(Arg_Types.size() != func.size() - 2) return false;//dif number of arguments
        int counter = 2;
        for(String arg : Arg_Types){
            if(!(arg.equals(func.get(counter))))
                if(!( isValidAssign(arg,func.get(counter),ExtendsMap) )) return false;
            counter++;
        }
        return true;
    }

    private boolean isValidAssign(String parent,String child,Map<String, String> ExtendsMap){
        String curCl = ExtendsMap.get(child);
        while(curCl != null){
            if(parent.equals(curCl)) return true;
            curCl = ExtendsMap.get(curCl);
        }
        return false;
    }

}


class FunctionCheck {
    private String className;
    private LinkedList<LinkedList<String>> fc_info;//[[f_name,type,arg_type1,...,arg_typeN],....]

    public FunctionCheck(String className){
        this.className = className;
        this.fc_info = new LinkedList<LinkedList<String>>();
    }

    public void fc_info_add(LinkedList<String> f){
        fc_info.add(f);
    }

    public LinkedList<LinkedList<String>> get_fc_info(){ return fc_info; }

    public void removeValue(List<String> value){
        fc_info.remove(value);
    }

}


class offsetClass {
    private Integer IdentifierCounter;
    private Integer FunctionCounter;
    private Integer IdentifierCounterStart;//remain
    private Integer FunctionCounterStart;//constant
    private String parent;
    private Map<String, Integer> INamesMap;
    private Map<String, Integer> FNamesMap;

    public offsetClass(offsetClass parent_offsets){
        if(parent_offsets != null){
            this.parent = parent_offsets.getparent();
            this.IdentifierCounter = new Integer(parent_offsets.getIdentifierCounter());
            this.FunctionCounter = new Integer(parent_offsets.getFunctionCounter());
        }
        else{
            this.parent = null;
            this.IdentifierCounter = new Integer(0);
            this.FunctionCounter = new Integer(0);
        }
        this.IdentifierCounterStart = new Integer(this.IdentifierCounter);
        this.FunctionCounterStart = new Integer(this.FunctionCounter);
        INamesMap = new LinkedHashMap<String, Integer>();
        FNamesMap = new LinkedHashMap<String, Integer>();
    }

    public void insertId(String name, String type){
        INamesMap.put(name,IdentifierCounter);
        IdentifierCounter = new Integer(IdentifierCounter.intValue() + this.findTypeSize(type));
    }

    private int findTypeSize(String type){
        if(type.equals("boolean")) return 1;
        if(type.equals("int")) return 4;
        return 8;//array or class
    }

    public void insertFunc(String name, int found){//if overriden found=parent's func offset else -1
        if(found == -1){
            FNamesMap.put(name,FunctionCounter);
            FunctionCounter += 8;
        }
        else{
            Integer f = new Integer(found);
            FNamesMap.put(name,f);
        }
    }

    public int getFuncOffset(String name) {
        return (FNamesMap.containsKey(name))?FNamesMap.get(name).intValue():-1 ;
    }

    public String getparent(){ return this.parent; }

    public int getIdentifierCounter(){ return this.IdentifierCounter.intValue(); }

    public int getFunctionCounter(){ return this.FunctionCounter.intValue(); }

    public boolean isEmpty(){
        if(this.IdentifierCounterStart.intValue() == this.IdentifierCounter.intValue() &&
           this.FunctionCounterStart.intValue() == this.FunctionCounter.intValue() ) return true;
        return false;
    }

    public void print_me(String className){
//        if(this.IdentifierCounterStart.intValue() != this.IdentifierCounter.intValue()){//2
            System.out.println("--Variables---");
            for(String IDname : INamesMap.keySet())
                System.out.println(className+"."+IDname+":"+INamesMap.get(IDname).intValue());
//        }//3
//        if(this.FunctionCounterStart.intValue() != this.FunctionCounter.intValue()){//4
            System.out.println("---Methods---");
            for(String Fname : FNamesMap.keySet()){
                int os = FNamesMap.get(Fname).intValue();
                if(os < this.FunctionCounterStart.intValue()) continue;//overriden method
                System.out.println(className+"."+Fname+":"+os);
            }
//        }//5
    }//1,2,3,4,5 comments to print or not empty classes,variables and methods 

}
