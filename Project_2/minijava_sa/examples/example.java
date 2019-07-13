class ex{
    public static void main(String[] xixi){
        System.out.println(new B().hello());
    }
}

class A{
    boolean x;

    public int hello(){
        return 1;
    }
}

class B extends A{
    int y;

    public int foo(){
        int ret;
        if(x){
            ret = 1;
        }
        else{
            ret = 2;
        }
        return ret;
    }
}

