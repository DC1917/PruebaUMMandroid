package com.stomas.appmqtt;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
//Librerias Mqtt y formulario
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Spinner;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
//Librerias Firebase
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    //Variables Firebase
    private EditText txtNombre, txtComp;
    private ListView lista;
    private Spinner spRegion, spGenero;
    //variable de la conexion de firestore
    private FirebaseFirestore db;
    //datos del spinner
    String[] Regiones = {"America", "Europa", "Asia"};
    String[] GeneroJuego = {"Accion", "Aventura", "RPG", "Terror", "Shooter", "Infantil"};

    //Variables de la conexion a MQTT
    private static String mqttHost = "tcp://prairieracer613.cloud.shiftr.io:1883";
    private static String IdUsuario = "AppAndroid";

    private static String Topico = "Mensaje";
    private static String User = "prairieracer613";
    private static String Pass = "vfB7ByfLAcpYo5td";

    //Variable que se utilizara para imprimir los datos del sensor
    private TextView textView;
    private EditText editTextMessage;
    private Button botonEnvio;

    //Libreria MQTT
    private MqttClient mqttClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //llamamos al metodo cargar lista
        CargarListaFirestore();
        //Inicializo Firestore
        db = FirebaseFirestore.getInstance();
        //uno las variables con los xml
        txtNombre = findViewById(R.id.txtNombre);
        txtComp = findViewById(R.id.txtComp);
        spRegion = findViewById(R.id.spRegion);
        spGenero = findViewById(R.id.spGenero);
        lista = findViewById(R.id.lista);
        //Poblar los spinner de los tipos de genero y regiones
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Regiones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRegion.setAdapter(adapter);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, GeneroJuego);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGenero.setAdapter(adapter2);
        //Enlace de la variable del id que esta en el activity main donde imprimiremos los datos
        textView = findViewById(R.id.textView);
        editTextMessage = findViewById(R.id.txtMensaje);
        botonEnvio = findViewById(R.id.botonEnviarMensaje);
        try {
            //Creacion de un cliente mqtt
            mqttClient = new MqttClient(mqttHost, IdUsuario, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(User);
            options.setPassword(Pass.toCharArray());
            //conexion al servidor mqtt
            mqttClient.connect(options);
            //si se conecta impimira un mensaje de mqtt
            Toast.makeText(this, "Aplicacion conectada al servidor MQTT", Toast.LENGTH_SHORT).show();
            //Manejo de entrega de datos y perdida de conexion
            mqttClient.setCallback(new MqttCallback() {
                //metodo en caso de que la conexion se pierda
                @Override
                public void connectionLost(Throwable cause) {
                    Log.d("MQTT", "Conexion perdida");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    runOnUiThread(() -> textView.setText(payload));

                }

                //Metodo para verificar si el envio fue exitoso
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d("MQTT", "Entrega Completa");
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
        //al dar click en el button enviara el mensaje del topico
        botonEnvio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //obtener el mensaje ingresado por el usuario
                String mensaje = editTextMessage.getText().toString();
                try {
                    //verifico si la conexion mqtt esta activa
                    if (mqttClient != null && mqttClient.isConnected()) {
                        //publicar el mensaje en el topico especificado
                        mqttClient.publish(Topico, mensaje.getBytes(), 0, false);
                        //mostrar el mensaje enviado en el textview
                        textView.append("\n - " + mensaje);
                        Toast.makeText(MainActivity.this, "Mensaje enviado", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Error: No se pudo enviar el mensaje. La conexion MQTT no esta activa", Toast.LENGTH_SHORT).show();
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });
    }
        public void enviarDatosFirestore(View view){
            String nombre = txtNombre.getText().toString();
            String compania = txtComp.getText().toString();
            String region = spRegion.getSelectedItem().toString();
            String genero = spGenero.getSelectedItem().toString();

            //Creamos un mapa con los datos a enviar
            Map<String, Object> juego = new HashMap<>();
            juego.put("nombre", nombre);
            juego.put("compania", compania);
            juego.put("region", region);
            juego.put("genero", genero);

            //enviamos los datos a firestore
            db.collection("juegos")
                    .document("nombre")
                    .set(juego)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MainActivity.this, "Datos enviados a Firestore correctamente", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Error al enviar los datos a firestore", Toast.LENGTH_SHORT).show();
                    });
        }
        public void CargarLista(View view){
            CargarListaFirestore();
        }
        public void CargarListaFirestore(){
            //Aqui el codigo para cargar la lista desde Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("juegos")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()){
                                List<String> listaJuegos = new ArrayList<>();
                                for (QueryDocumentSnapshot document : task.getResult()){
                                    String linea = "||" + document.getString("nombre") + "||" +
                                            document.getString("compania");
                                    listaJuegos.add(linea);
                                }
                                ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                                        MainActivity.this,
                                        android.R.layout.simple_list_item_1,
                                        listaJuegos
                                );
                                lista.setAdapter(adaptador);
                            }else {
                                Log.e("TAG", "Error al obtener los datos de Firestore", task.getException());
                            }
                        }
                    });


    }
}