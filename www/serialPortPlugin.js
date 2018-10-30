var exec = require('cordova/exec');

//下面的serialPortPlugin对应我们plugin.xml里面对应的js-module里面的name,
    // <js-module
    //     name="serialPortPlugin"
    //     src="www/serialPortPlugin.js">
    //     <clobbers target="cordova.plugins.SerialPortPlugin" />
    // </js-module>

    
// openSerialPort对应我们Android原生代码里面的方法中action要等于的标志
//     @Override
//     public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
//         //获取上下文
//         Context context = cordova.getActivity().getApplicationContext();
//         if (action.equals("openSerialPort")) {
//             Log.i("serialPortPlugin", "==========================进入插件成功==========================");
//             if (args != null && args.length() > 0) {
//                 //服务器ip地址
//                 serverIp=args.getString(0);
// //                serverIp = "http://192.168.2.5:8080/api";
//                 Log.i("serialPortPlugin", "serverIp===" + serverIp);
//                 //获取班牌ip
//                 brandClassIp = args.getString(1);
//                 Log.i("serialPortPlugin", "brandClassIp===" + brandClassIp);

//             } else {
//                 //服务器ip或班牌ip不存在
//                 android.widget.Toast.makeText(cordova.getActivity(), "网络异常....", Toast.LENGTH_SHORT).show();
//             }



exports.openSerialPort = function (arg0, success, error) {
    exec(success, error, 'serialPortPlugin', 'openSerialPort', [msg]);
};




