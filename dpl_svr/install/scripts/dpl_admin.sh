cpath="/var/lib/tomcat6/webapps/dpl/WEB-INF/lib/"
args="$@ -c /var/lib/tomcat6/webapps/dpl/WEB-INF/conf/dopple.conf"
java -cp $cpath/geof_admin.jar:$cpath/* org.geof.admin.GeofAdmin "$args"
