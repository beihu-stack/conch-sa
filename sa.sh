#! /bin/sh
mkdir "/tmp/sa-code"
chown adam /tmp/sa-code

java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo "$java_version"
major_version=$(echo "$java_version" | awk -F '.' '{print $1}')
echo "$major_version"

pid=$(pgrep -f "java")

if ((major_version >= 9)); then
  echo "Java version is greater than or equal to 9."
  mv ./conch-sa-core-v9/src/main/java/com/nabob/conch/sa/core/v9/JvmMethodCounter.java /tmp/sa-code/JvmMethodCounter.java
  javac --add-modules jdk.hotspot.agent --add-exports jdk.hotspot.agent/sun.jvm.hotspot=ALL-UNNAMED --add-exports jdk.hotspot.agent/sun.jvm.hotspot.classfile=ALL-UNNAMED --add-exports jdk.hotspot.agent/sun.jvm.hotspot.oops=ALL-UNNAMED --add-exports jdk.hotspot.agent/sun.jvm.hotspot.runtime=ALL-UNNAMED --add-exports jdk.hotspot.agent/sun.jvm.hotspot.utilities=ALL-UNNAMED /tmp/sa-code/JvmMethodCounter.java
  chmod 777 /tmp/sa-code/JvmMethodCounter.class
else
  echo "Java version is less than 9."
  chmod 777 /usr/java/jdk1.8/lib/sa-jdi.jar
  mv ./conch-sa-core-v8/src/main/java/com/nabob/conch/sa/core/v8/JvmMethodCounter.java /tmp/sa-code/JvmMethodCounter.java
  javac -cp /usr/java/jdk1.8/lib/sa-jdi.jar:. /tmp/sa-code/JvmMethodCounter.java
  chmod 777 /tmp/sa-code/JvmMethodCounter.class
fi

jdk_path=$(find /usr/java -maxdepth 1 -name "jdk*" | head -n 1)

if ((major_version >= 9)); then
  echo "Java version is greater than or equal to 9."
  sudo -u adam "$jdk_path"/bin/java --add-modules jdk.hotspot.agent --add-exports jdk.hotspot.agent/sun.jvm.hotspot=ALL-UNNAMED --add-exports jdk.hotspot.agent/sun.jvm.hotspot.classfile=ALL-UNNAMED --add-exports jdk.hotspot.agent/sun.jvm.hotspot.oops=ALL-UNNAMED --add-exports jdk.hotspot.agent/sun.jvm.hotspot.runtime=ALL-UNNAMED --add-exports jdk.hotspot.agent/sun.jvm.hotspot.utilities=ALL-UNNAMED --class-path /tmp/sa-code JvmMethodCounter "$pid"
else
  echo "Java version is less than 9."
  sudo -u adam "$jdk_path"/bin/java -cp /usr/java/jdk1.8/lib/sa-jdi.jar:/tmp/sa-code JvmMethodCounter "$pid"
fi

csv_name=$(find /tmp/sa-code -type f -name "*.csv" | head -n 1)
# deploy csv file

# delete csv file
rm -f "$csv_name"
