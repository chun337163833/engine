关于配置文件的改写
可以通过参数
-Dconfig.files=完整路径,${repeat}
-Dconfig.relativeFiles=相对路径,${repeat}
-Dconfig.resources=资源文件,${repeat}
-Dconfig.urls=网络地址,${repeat}
来重写参数
该文件在dist后会通过
mappings in Universal += baseDirectory.value / "src/main/resources/application.conf" -> "conf/application.conf"
具体的结构为：
.
├── bin
│   ├── pay
│   └── pay.bat
├── conf
│   └── application.conf
└── lib
