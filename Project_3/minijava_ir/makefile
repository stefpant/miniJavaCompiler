all: compile

compile:
	java -jar ../../jtb132di.jar -te minijava.jj
	java -jar ../../javacc5.jar minijava-jtb.jj
	javac MyFillSTVisitor.java
	javac SymbolTable.java
	javac TypeCheckVisitor.java
	javac IntRepVisitor.java
	javac Main.java

clean:
	rm -f *.class *~ JavaCharStream.java minijava-jtb.jj MiniJavaParser.java
	rm -f MiniJavaParserConstants.java MiniJavaParserTokenManager.java
	rm -f ParseException.java Token.java TokenMgrError.java
	rm -rf syntaxtree visitor
