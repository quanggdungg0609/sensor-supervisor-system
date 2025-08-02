package main

import (
	"encoding/json"
	"fmt"
	"log"
	"math/rand"
	"os"
	"os/signal"
	"syscall"
	"time"

	mqtt "github.com/eclipse/paho.mqtt.golang"
)

// --- Configuration ---
// Replace these values with your actual system credentials and broker address.
const (
	mqttBroker   = "tcp://localhost:1883" // Example: "tcp://your_broker_host:1883"
	clientID     = "XVRAIF6Y"
	username     = "esp321"
	password     = "=7uOp3N6"
	publishTopic = "sensors/%s/telemetry" // Topic format, %s will be replaced by clientID
)

// --- Structs for JSON Payload ---

// SensorData represents the nested "data" object in the JSON.
type SensorData struct {
	Temp     float64 `json:"temp"`
	Humidity int     `json:"humidity"`
}

// MQTTPayload represents the top-level JSON structure.
type MQTTPayload struct {
	Timestamp string     `json:"timestamp"`
	Data      SensorData `json:"data"`
}

// --- MQTT Handlers ---

// connectHandler is called when the client successfully connects to the broker.
var connectHandler mqtt.OnConnectHandler = func(client mqtt.Client) {
	log.Println("Successfully connected to MQTT broker!")
}

// connectionLostHandler is called when the client loses its connection.
var connectionLostHandler mqtt.ConnectionLostHandler = func(client mqtt.Client, err error) {
	log.Printf("Connection lost: %v\n", err)
}

func main() {
	// --- Set up MQTT client options ---
	opts := mqtt.NewClientOptions()
	opts.AddBroker(mqttBroker)
	opts.SetClientID(clientID)
	opts.SetUsername(username)
	opts.SetPassword(password)
	opts.SetDefaultPublishHandler(messagePubHandler)
	opts.OnConnect = connectHandler
	opts.OnConnectionLost = connectionLostHandler

	// --- Create and connect the client ---
	client := mqtt.NewClient(opts)
	if token := client.Connect(); token.Wait() && token.Error() != nil {
		log.Fatalf("Failed to connect to MQTT broker: %v", token.Error())
	}

	// --- Start publishing data every 10 seconds ---
	publishData(client)
}

// publishData contains the main loop for publishing messages periodically.
func publishData(client mqtt.Client) {
	// Create a ticker that fires every 10 seconds.
	ticker := time.NewTicker(10 * time.Second)
	defer ticker.Stop()

	// Set up a channel to listen for OS signals for graceful shutdown.
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)

	log.Println("Starting publisher... Press Ctrl+C to exit.")

	// Main loop
	for {
		select {
		case <-ticker.C:
			// This block runs every 10 seconds.
			payload := createPayload()
			jsonPayload, err := json.Marshal(payload)
			if err != nil {
				log.Printf("Error marshalling JSON: %v\n", err)
				continue
			}

			// Construct the topic with the actual clientID.
			topic := fmt.Sprintf(publishTopic, clientID)

			// Publish the message.
			token := client.Publish(topic, 1, false, jsonPayload)
			token.Wait() // Wait for the publish to complete.

			log.Printf("Published message to topic '%s': %s\n", topic, jsonPayload)

		case <-sigChan:
			// This block runs when Ctrl+C is pressed.
			log.Println("Shutdown signal received. Disconnecting...")
			client.Disconnect(250)
			log.Println("Publisher disconnected.")
			return
		}
	}
}

// createPayload generates a new payload with the current time and random sensor data.
func createPayload() MQTTPayload {
	return MQTTPayload{
		Timestamp: time.Now().UTC().Format(time.RFC3339),
		Data: SensorData{
			Temp:     25.0 + rand.Float64()*(5.0), // Random temp between 25.0 and 30.0
			Humidity: 40 + rand.Intn(20),          // Random humidity between 40 and 60
		},
	}
}

// messagePubHandler is a default handler for incoming messages (not used for publishing).
var messagePubHandler mqtt.MessageHandler = func(client mqtt.Client, msg mqtt.Message) {
	log.Printf("Received message: %s from topic: %s\n", msg.Payload(), msg.Topic())
}
