package com.running.serialport;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;


import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import android_serialport_api.SerialPort;
import com.running.entity.KqInfor;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.running.utils.ChangeTool;
import com.running.utils.SQLiteOpenHelperUtil;
import com.running.utils.SerialPortUtils;

/**
 * This class echoes a string called from JavaScript.
 */
public class SerialPortPlugin extends CordovaPlugin {

    //端口工具类
    private SerialPortUtils serialPortUtils = new SerialPortUtils();
    private SerialPort serialPort;

    //handler
    private Handler handler;
    //字节流
    private byte[] mBuffer;

    //服务器IP域名
    private String serverIp;

    //班牌IP
    private String brandClassIp;

    //设置请求的连接和读入的时间
    final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        //获取上下文
        Context context = cordova.getActivity().getApplicationContext();
        if (action.equals("openSerialPort")) {
            Log.i("serialPortPlugin", "==========================进入插件成功==========================");
            if (args != null && args.length() > 0) {
                //服务器ip地址
                serverIp=args.getString(0);
//                serverIp = "http://192.168.2.5:8080/api";
                Log.i("serialPortPlugin", "serverIp===" + serverIp);
                //获取班牌ip
                brandClassIp = args.getString(1);
                Log.i("serialPortPlugin", "brandClassIp===" + brandClassIp);

            } else {
                //服务器ip或班牌ip不存在
                android.widget.Toast.makeText(cordova.getActivity(), "网络异常....", Toast.LENGTH_SHORT).show();
            }

            //打开串口
            this.opend();

            //实例化handler
            handler = new Handler();

            //串口数据监听事件
            serialPortUtils.setOnDataReceiveListener(new SerialPortUtils.OnDataReceiveListener() {
                @Override
                public void onDataReceive(byte[] buffer, int size) {
                    mBuffer = buffer;
                    handler.post(runnable);
                }

                //开线程更新UI
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {

                        //获取上下文
                        Context context = cordova.getActivity().getApplicationContext();
                        //IC卡数据
                        String icString = ChangeTool.ByteArrToHex(mBuffer, 0, 7);
                        //获取当前打卡时间时间
                        Date date = new Date();
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String d = format.format(date);
                        //判断当前网络状态：如果当前网络良好，则将数据传输给服务器
                        if (isNetworkConnected(context)) {
                            //每次打前都检查一下数据库表是否有数据如果有数据取出，然后上传到服务器
                            // 创建DatabaseHelper对象
                            SQLiteOpenHelperUtil dbHelper = new SQLiteOpenHelperUtil(context, "kqdb", 2);
                            // 调用getWritableDatabase()方法创建或打开一个可以读的数据库
                            SQLiteDatabase sqliteDatabase = dbHelper.getReadableDatabase();
                            // 调用SQLiteDatabase对象的query方法进行查询
                            // 返回一个Cursor对象：由数据库查询返回的结果集对象
                            Cursor cursor = sqliteDatabase.query("kq_records", new String[]{"student_uuid",
                                    "datetime"}, null, null, null, null, null);
                            String student_uuid = null;
                            String datetime = null;
                            //List存储考勤数据
                            List<KqInfor> listKqInfor = new ArrayList<KqInfor>();
                            //将光标移动到下一行，从而判断该结果集是否还有下一条数据
                            //如果有则返回true，没有则返回false
                            while (cursor.moveToNext()) {
                                student_uuid = cursor.getString(cursor.getColumnIndex("student_uuid"));
                                datetime = cursor.getString(cursor.getColumnIndex("datetime"));
                                //输出查询结果
                                Toast.makeText(cordova.getActivity(), "数据库取出数据：" + " student_uuid:" + student_uuid + " datetime:" + datetime, Toast.LENGTH_SHORT).show();
                                android.widget.Toast.makeText(cordova.getActivity(), "班牌IP地址：" + brandClassIp, Toast.LENGTH_SHORT).show();
                                //调用网络数据传输方法将数据上传
                                try {
                                    postRequest("0",serverIp, brandClassIp, student_uuid, datetime);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            //读取完后清空数据库
                            delete("kqdb", 2, "kq_records");
                            //关闭数据库
                            sqliteDatabase.close();

                            Log.i("serialPortPlugin", "==========================当前网络良好==========================");
                            //打卡成功
                            android.widget.Toast.makeText(cordova.getActivity(), "打卡签到成功" , Toast.LENGTH_SHORT).show();
                            android.widget.Toast.makeText(cordova.getActivity(), "打卡签到成功" + icString, Toast.LENGTH_SHORT).show();
                            android.widget.Toast.makeText(cordova.getActivity(), "打卡签到时间" + d, Toast.LENGTH_SHORT).show();
//                            android.widget.Toast.makeText(cordova.getActivity(), "班牌IP地址：" + brandClassIp, Toast.LENGTH_SHORT).show();
                            // 调用网络数据传输方法将数据上传
                            try {
                                postRequest("1",serverIp, brandClassIp, icString, d);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {//将数据暂时保存到数据库
                            Log.i("serialPortPlugin", "==========================当前网络不好==========================");
                            android.widget.Toast.makeText(cordova.getActivity(), "打卡签到成功" , Toast.LENGTH_SHORT).show();
                            android.widget.Toast.makeText(cordova.getActivity(), "网络不好", Toast.LENGTH_SHORT).show();
                            write(icString, d);
                        }
                    }
                };
            });
            return true;
        }
        return false;
    }


    //打开串口
    public void opend() {
        TimerTask task = new TimerTask() {
            public void run() {
                Message msg = Message.obtain();
                msg.what = 3;
                handlerTest.sendMessage(msg);
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 2000);//2秒后执行TimeTask的run方法
    }


    private Handler handlerTest = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 3) {//自动打开串口
                serialPort = serialPortUtils.openSerialPort();
                if (serialPort == null) {
                    Log.d("serialPortPlugin", "开启串口失败");
                    return;
                } else {
                    Log.i("serialPortPlugin", "开启串口成功");
                }
            }
        }
    };


    //判断当前网络是否通顺
    public boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }


    private void postRequest(String connect,String serverIp, String brandClassIp, String studentUuid, String arrivalTime) throws IOException {
        Log.i("serialPortPlugin", "=========================================");
        Log.i("serialPortPlugin", connect);
        Log.i("serialPortPlugin", serverIp);
        Log.i("serialPortPlugin", brandClassIp);
        Log.i("serialPortPlugin", studentUuid);
        Log.i("serialPortPlugin", arrivalTime);

        //传输参数
        RequestBody formBody = new FormBody.Builder()
                .add("connect", connect)
                .add("brandClassIp", brandClassIp)
                .add("studentUuid", studentUuid)
                .add("arrivalTime", arrivalTime)
                .build();
        //post请求路径
        final Request request = new Request.Builder()
                .url(serverIp + "/KqSystem/getKqInfor.do")
                .post(formBody)
                .build();

        //开启线程处理请求返回的数据
        new Thread(new Runnable() {
            @Override
            public void run() {
                Response response = null;
                try {
                    response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        //存入内存
                        String resultString = response.body().string();
                        Log.i("intenet", "=====================================================");
                        Log.i("intenet", "打印POST响应的数据：" + resultString);
                        Message msg = Message.obtain();
                        msg.what = 4;
                        Bundle bundle = new Bundle();
                        bundle.putString("postResult", resultString);
                        msg.setData(bundle);
                        handlerTest.sendMessage(msg);
                    } else {
                        throw new IOException("Unexpected code " + response);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }


    /**
     * 向数据库写入数据
     *
     * @param icString
     * @param d
     */
    public void write(String icString, String d) {
        //获取上下文
        Context context = cordova.getActivity().getApplicationContext();

        //创建SQLiteOpenHelper子类对象
        SQLiteOpenHelperUtil dbHelper = new SQLiteOpenHelperUtil(context, "kqdb", 2);
        // 调用getWritableDatabase()方法创建或打开一个可以读的数据库
        SQLiteDatabase sqliteDatabase = dbHelper.getWritableDatabase();
        // 创建ContentValues对象
        ContentValues values = new ContentValues();
        // 向该对象中插入键值对
        values.put("student_uuid", icString);
        values.put("dateTime", d);
        sqliteDatabase.insert("kq_records", null, values);
        //关闭数据库
        sqliteDatabase.close();
    }


    /**
     * 清空数据表数据
     *
     * @param dbName
     * @param version
     * @param tableName
     */
    public void delete(String dbName, Integer version, String tableName) {

        //获取上下文
        Context context = cordova.getActivity().getApplicationContext();

        // 创建DatabaseHelper对象
        SQLiteOpenHelperUtil dbHelper = new SQLiteOpenHelperUtil(context, dbName, version);

        // 调用getWritableDatabase()方法创建或打开一个可以读的数据库
        SQLiteDatabase sqliteDatabase = dbHelper.getWritableDatabase();

        //删除数据
        sqliteDatabase.delete(tableName, null, null);

        //关闭数据库
        sqliteDatabase.close();
    }





}
