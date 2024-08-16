Linux:

javac -cp *.jar *.java -d .

java -cp .:* Main input_arguments.dat

Windows Powershell:

javac -cp *.jar *.java -d .

java -cp '.;*' Main input_arguments.dat

Windows CMD:

javac -cp *.jar *.java -d .

java -cp .;* Main input_arguments.dat
