#!/usr/bin/bash

BASE=$(dirname $0)
JVM_CONFIG="-Xms512m -Xmx2g"

for f in $BASE/../lib; do
    CLASS_PATH=${CLASS_PATH}:$f;
done

usage() {
    echo "[Usage] sh $0 [BloomFilterMake|BloomFilterTest|DataGenerate|IndexBenchmark|IndexFileMake|IndexFileText]\n";
}

if [ $# -lt 1 ];then
    usage
    exit 1
fi

command=$1
case "$command" in
    BloomFilterMake)
        CLASS=me.johntse.toy.index.tools.BloomFilterCreator
        ;;
    BloomFilterTest)
        CLASS=me.johntse.toy.index.tools.BloomFilterTester
        JVM_CONFIG="-server -XX:+AlwaysPreTouch -XX:+PrintCommandLineFlags -Xloggc:/home/gc-%t.log -XX:NumberOfGCLogFiles=5 -XX:+UseGCLogFileRotation -XX:GCLogFileSize=20m -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintHeapAtGC -XX:+PrintGCCause -XX:+PrintTenuringDistribution -Xmn1g -Xms2g -Xmx4g -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=64m -XX:-UseAdaptiveSizePolicy -XX:SurvivorRatio=10 -XX:+UseConcMarkSweepGC -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=85 -XX:+ExplicitGCInvokesConcurrent -XX:+CMSScavengeBeforeRemark -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/home/index.hprof -XX:ErrorFile=/home/hs_err_pid%p.log"
        ;;
    DataGenerate)
        CLASS=me.johntse.toy.index.tools.DataGenerator
        ;;
    IndexBenchmark)
        CLASS=me.johntse.toy.index.tools.IndexBenchmarkTester
        JVM_CONFIG="-server -XX:+AlwaysPreTouch -XX:+PrintCommandLineFlags -Xloggc:/home/gc-%t.log -XX:NumberOfGCLogFiles=5 -XX:+UseGCLogFileRotation -XX:GCLogFileSize=20m -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintHeapAtGC -XX:+PrintGCCause -XX:+PrintTenuringDistribution -Xmn1g -Xms5g -Xmx5g -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=64m -XX:-UseAdaptiveSizePolicy -XX:SurvivorRatio=10 -XX:+UseConcMarkSweepGC -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=85 -XX:+ExplicitGCInvokesConcurrent -XX:+CMSScavengeBeforeRemark -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/home/index.hprof -XX:ErrorFile=/home/hs_err_pid%p.log"
        ;;
    IndexFileMake)
        CLASS=me.johntse.toy.index.tools.IndexFileMaker
        ;;
    IndexFileText)
        CLASS=me.johntse.toy.index.tools.IndexFileText
        ;;
    *)
        usage
        exit 1
esac

exec java $JVM_CONFIG -cp $CLASS_PATH $CLASS "$@"