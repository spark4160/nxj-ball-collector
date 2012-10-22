cd src/
ls
for filename in $(ls *.java)
do
	class=${filename%.java}
	echo "$class"
	nxjc $class.java
	echo "1"
	nxjlink -o $class.nxj $class
	echo "2"
	nxjupload $class.nxj
	echo "3"

done
