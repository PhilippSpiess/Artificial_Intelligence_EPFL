# AI projects I have been working on during my Master's at EPFL

## Model Predictive Control
In a team of three, we implemented model predictive control in Matlab to simulate a quadcopter with non-linear dynamics tracking a references and successfully rejecting noise.

## Data Science for Business
In a team of three and at the end of the course « Data science for business », we implemented a
jupyter notebook with all the steps necessary to predict whether companies with specific attributes were likely to observe the price rise or fall after their Initial Public Offering. The predictions are based on an existing dataset of many companies with their pre and post IPO valuations. Two significant steps of the project involved data preprocessing and text analysis with NLP. We predicted the probabilities that the price would go up or down and even the price itself. The AUC score we obtained for the first task is 0.7. It allows us to think that the model could be used to filter the best opportunities in the market.

## Intelligent Agents
In a group of two, we first implemented in Java a reactive and a deliberative agent in a task-delivery problem using respectively the value iteration and the A* algorithms. We then tackled the coordination of multiple agents using the Stochastic Local Search algorithm. Finally, we competed against the other groups of the class in an auction game. We came out with the second best code (out of 75 groups participating).

## Body-robot Mapping (at the Laboratory of Intelligent Systems)
For my semester project, I studied how people could control robots using their body. First, I developed
two simulators in UNITY-3D and programed a rover and a quadcopter trajectory. I then asked 15 people to experiment the simulators to see how they would imitate the robots. Gigabytes of time series Data was collected using a motion capture system and then processed with Python. Using a correlation analysis and PCA, I was able to select relevant body parts involved in robot control. My results suggested a single way to control a land-rover using the torso only and two different ways to intuitively control a quadcopter.

## Reinforcement Learning
In a group of two, we used the Monte Carlo Policy-gradient algoritm using a deep neural network as a function approximator to teach a simulated spaceship how to « land on the moon ». The network takes as input 8 states and returns 4 control output. We also implemented a value network to increase the performance of the program : an adaptive baseline. We then used entropy to regulate the exploration/exploitation dilemma. As a result, using the regulation, the starship was able to land safely in the simulated environment after 1000 iterations.
