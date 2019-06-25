// pedsim - A microscopic pedestrian simulation system.
// Copyright (c) by Christian Gloor


//g++ examples/trab2.cpp -o trab -lpedsim -L. -I. -std=c++11
//export LD_LIBRARY_PATH=.
//./trab

#include <iostream>
#include <cstdlib>
#include <chrono>
#include <thread>

#include "ped_includes.h"

#include "ped_outputwriter.h"

using namespace std;

int main(int argc, char *argv[]) {

    // create an output writer which will send output to a file
    Ped::OutputWriter *ow = new Ped::UDPOutputWriter();
    ow->setScenarioName("Manauara Shopping Simulator2");

    cout << "PedSim Exemplo utilizando a versao libpedsim " << Ped::LIBPEDSIM_VERSION << endl;

    // Setup CENA - Andar modelado sem obstáculo
    Ped::Tscene *pedscene = new Ped::Tscene(0, 0, 100, 30); // essa é a cena (x1,y1) (x2,y2)
    pedscene->setOutputWriter(ow);

    //Escrevendo dados no arquivo que vai pro the one
    cout << "360 360 0 100 0 30 0 0" << '\n';


    //PORTAS MANAUARA (X,Y,raio)
    Ped::Twaypoint *w2 = new Ped::Twaypoint(100, 20, 5);
    Ped::Twaypoint *w1 = new Ped::Twaypoint(0, 10, 5);

    // setup OBSTACULO
    pedscene->addObstacle(new Ped::Tobstacle(10, 10, 10, 20));//obstaculo 1
    pedscene->addObstacle(new Ped::Tobstacle(10, 10, 20, 10));
    pedscene->addObstacle(new Ped::Tobstacle(10, 20, 20, 10));

    pedscene->addObstacle(new Ped::Tobstacle(10, 30, 30, 15));//obstaculo 2
    pedscene->addObstacle(new Ped::Tobstacle(30, 15, 60, 30));
    pedscene->addObstacle(new Ped::Tobstacle(20, 0, 30, 10));
    pedscene->addObstacle(new Ped::Tobstacle(30, 10, 40, 0));

    pedscene->addObstacle(new Ped::Tobstacle(40, 10, 60, 10));//obstaculo 3
    pedscene->addObstacle(new Ped::Tobstacle(40, 10, 60, 20));
    pedscene->addObstacle(new Ped::Tobstacle(60, 10, 60, 20));

    pedscene->addObstacle(new Ped::Tobstacle(70, 10, 90, 10));//obstaculo 4
    pedscene->addObstacle(new Ped::Tobstacle(70, 10, 70, 20));
    pedscene->addObstacle(new Ped::Tobstacle(70, 20, 90, 10));

    pedscene->addObstacle(new Ped::Tobstacle(-10, 0, -10, 30));//obstaculo 5 contorno
    pedscene->addObstacle(new Ped::Tobstacle(110,30, -10, 30));
    pedscene->addObstacle(new Ped::Tobstacle(110,30, 110,  0));
    pedscene->addObstacle(new Ped::Tobstacle(-10, 0, 110,  0));


    //NUMERO DE PESSOAS INICIAIS = 100; máximo = 700
    //Ped::Tagent *a = new Ped::Tagent();
    for (int i = 0; i<100; i++) {//pessoas
        Ped::Tagent *a = new Ped::Tagent();
        //Onde o ponto vai surgir, na ENTRADA
        if (i%2==0) {
            a->addWaypoint(w1);
            double x = (double)(90.0 + rand()/(RAND_MAX/10.0));
            double y = (double)(10.0 + rand()/(RAND_MAX/10.0));
            a->setPosition( x, y, 0.0 );
            cout << 0 << ' ' << a->getid() << ' ' << x << ' ' << y <<'\n';
        }else {
            a->addWaypoint(w2);
            double x = (double)(0.0 + rand()/(RAND_MAX/10.0));
            double y = (double)(10.0 + rand()/(RAND_MAX/10.0));
            a->setPosition( x, y, 0.0 );
            cout << 0 << ' ' << a->getid() << ' ' << x << ' ' << y <<'\n';
        }

        a->setVmax(1.5);//velocidade
        pedscene->addAgent(a);

    }

    //cout << "Saiu do primeiro for" << '\n';

    // TEMPO 6h = 360min = 21600 segundos
    //Move all agents for 700 steps (and write their position through the outputwriter)
    for (int i=1; i<=21600; i++) {
        //cout << "Entrou no segundo for."<< '\n';
        if (i%900==0) { //a cada 15min tem q renovar com mais 25 pessoas
		        //cout << "Entrou no if."<< '\n';
            for (int j = 0; j<25; j++) {//pessoas
                Ped::Tagent *a = new Ped::Tagent();
                //Onde o ponto vai surgir, na ENTRADA
                if (j%2==0) {
                    a->addWaypoint(w1);
                    double x = (double)(90.0 + rand()/(RAND_MAX/10.0));
                    double y = (double)(10.0 + rand()/(RAND_MAX/10.0));
                    a->setPosition( x, y, 0.0 );
                }else {
                    a->addWaypoint(w2);
                    double x = (double)(0.0 + rand()/(RAND_MAX/10.0));
                    double y = (double)(10.0 + rand()/(RAND_MAX/10.0));
                    a->setPosition( x, y, 0.0 );
                }

                a->setVmax(1.5);//velocidade
                pedscene->addAgent(a);

            }

        }

        for(Ped::Tagent *agente : pedscene->getAllAgents()){
          if(!agente->reachedDestination()) { //se o agente chegar no destino não imprima ele (como se tivesse deletado)
            cout << i << ' ' << agente->getid() << ' ' << (agente->getPosition()).x << ' ' << (agente->getPosition()).y <<'\n';
            }
        }
        pedscene->moveAgents(0.3 + rand()/(RAND_MAX/1.5) - 1.5);//velocidade de movimentação das pessoas
        std::this_thread::sleep_for(std::chrono::milliseconds(3));
    }

    // int notreached = myagents.size();
    // while (notreached > 0) {
    //     timestep++;
    //     notreached = myagents.size();
    //     pedscene->moveAgents(0.4);

    //     for (auto a : myagents) if (a->reachedDestination()) delete a;
    //     if (timestep >= 20000) notreached = 0; // seems to run forever.
    // }

    // Cleanup
    for (auto a : pedscene->getAllAgents()) { delete a; };
    for (auto o : pedscene->getAllObstacles()) { delete o; };
    delete pedscene;
    delete w1;
    delete w2;
    delete ow;

    return EXIT_SUCCESS;
}
