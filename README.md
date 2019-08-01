# Toy-Index-File

源于一个数据查询问题。

```
现有一文本文件，文件内容每行记录由3个字段组成，字段间以'\t'分隔，每行以'\n'分隔，3个字段分别为”身份证号码 手机号码 姓名“。现需要提供一个服务，业务可通过服务根据身份证号码或者手机号码精确查询相关记录。那么如何编程设计，实现响应低、吞吐高和资源消耗低的查询。
```

具体设计，可以阅读我的这篇博文[一次数据查询设计实现始末](https://johntse.me/2019/07/22/a-index-for-data-query-design-and-implementation/)。

## 1. 编译

```shell
$ gradle jar
```

## 2. 使用

### 生成数据

```shell
$ sh bin/index.sh DataGenerate
生成测试数据。
[Usage] DataGenerate [records_num] [output_file] [name_file] [name_encoded] <encoding>
	records_num: 生成的记录条数。
	output_file: 生成的数据文件。
	name_file: 姓名文件，用于生成记录中的姓名。
	name_encoded: 是否对姓名进行编码，即生成的记录中使用姓名编码，而非姓名。
	encoding: 文件编码方式，默认为'utf8'
```

### 构建Bloom Filter

```shell
$ sh bin/index.sh BloomFilterCreate
构建Bloom Filter。
[Usage] BloomFilterCreate [data_file] [index] [expected_fpp] [record_num] [output_file]
	data_file: 构建时使用的数据文本文件
	index: 构建时使用的数据文本文件使用数据文本文件中哪一列
	expected_fpp: 期望的最大误判率
	record_num: 构建时使用的记录数
	output_file: 输出的Bloom Filter过滤器序列化二进制文件
```

### 构建索引文件

```shell
$ sh bin/index.sh IndexFileMake
构建索引文件。
[Usage] IndexFileMake [index_type] [text_input_file] [idx] <range1,range2,range3> <field_num, field_size, field_size, field_size, ...> <charset> 
	index_type: 填写值包括SFZ, MOBILE, NAME，分别表示身份证号码、手机号码和姓名
	text_input_file: 索引字段（idx）已排序，姓名已替换为编号的原始文本文件
	idx: text_input_file中索引字段所在下标，该字段格式为'一级索引,二级索引,内容前缀'
	range1: 一级索引表示，格式为M-N:size，即从索引字段如何生成一级索引。从下标M开始（包含）到下标N结束（包含）为一级索引,其大小为size个字节。index_type=NAME时，可不填
	range2: 二级索引表示，格式为M-N:size，即从索引字段如何生成二级索引。从下标M开始（包含）到下标N结束（包含）为二级索引,其大小为size个字节。index_type=NAME时，可不填
	range3: 内容前缀表示，格式为M-N:size，即从索引字段如何生成内容前缀。从下标M开始（包含）到下标N结束（包含）为内容前缀,其大小为size个字节。index_type=NAME时，可不填
	field_num: 内容字段个数
	field_size: 内容字段大小表示，格式为Index:size，即内容第Index个字段，其大小为size个字节。该配置项为多值，具体数量和内容中字段个数有关。index_type=NAME时，可不填
	charset: 原始文本文件的字符编码，可选值。默认为'utf8'
```

### 索引文件文本化

```shell
$ sh bin/index.sh IndexFileText
将二进制格式的索引文件文本化输出。
[Usage] IndexFileText [index_type] [index_file]
	index_type: 填写值包括SFZ, MOBILE, NAME，分别表示身份证号码、手机号码和姓名索引。
	index_file: 需要文本化的索引文件。
```

### 索引文件性能测试

```shell
$ sh bin/index.sh IndexBenchmark
索引文件性能测试。
[Usage] IndexBenchmark [index_type] [index_file] [test_file] [batch_size] <name_index_file> <bloom_filter_bin>
	index_type: 填写值包括SFZ, MOBILE, NAME，分别表示身份证号码、手机号码和姓名索引测试
	index_file: 和测试类型对应的索引文件
	test_file: 测试文本文件，模拟查询，多个文件时使用','分隔
	batch_size: 每次从磁盘读取的记录条数
	name_index_file: 姓名索引文件，当测试类型为SFZ和MOBILE时，需要设置
	bloom_filter_bin: bloom filter二进制文件，用于构造bloom filter，不设置时表示不使用bloom filter。当测试类型为SFZ和MOBILE时，设置有效
```

