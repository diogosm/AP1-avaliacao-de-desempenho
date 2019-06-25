import csv
import json
import copy

def convert_to_dic(path, save=True):
    events = []
    with open(path) as raw_file:
        csv_obj = csv.reader(raw_file)
        for row in csv_obj:
            # print(row)
            connection_event = {
                "node1": int(row[0])-1,
                "node2": int(row[1])-1,
                "start_time": row[2],
                "duration": row[3]
            }
            events.append(connection_event)
    if save:
        with open("connection_events.json", "w+") as json_file:
            json.dump(events, json_file, indent=4, ensure_ascii=False)

    return events

def gerate_the_one_file(input_file, save=True):
    conn_events = convert_to_dic(input_file)
    conn_events = sorted(conn_events, key=lambda value: value["start_time"])
    
    global_start_time = float(conn_events[0]["start_time"])
    
    events_in_secs = []

    for event in conn_events:
        cur_up_dict = copy.deepcopy(event)
        cur_down_dict = copy.deepcopy(event)
        cur_up_dict["type"] = "UP"
        cur_down_dict["type"] = "DOWN"
        cur_up_dict["sec_time"] = (float(event["start_time"])- global_start_time) / 1000.0
        cur_down_dict["sec_time"] = ((float(event["start_time"]) + float(event["duration"])) - global_start_time) / 1000.0
        events_in_secs.append(cur_up_dict)
        events_in_secs.append(cur_down_dict)

    events_in_secs = sorted(events_in_secs, key=lambda value: value["sec_time"])
    
    if(save):
        with open("ordered_in_secs.json", "w+") as ordered:
            json.dump(events_in_secs, ordered, indent=4, ensure_ascii=False)
    
    with open("hycupps_trace.txt", "a+") as trace_file:
        for event in events_in_secs:
            trace_file.write(
                "{time} CONN {node1} {node2} {type}\n"
                .format(time=event["sec_time"], node1=event["node1"], node2=event["node2"], type=event["type"]))
    
    
if __name__ == "__main__":
    gerate_the_one_file("./full_output.txt")
