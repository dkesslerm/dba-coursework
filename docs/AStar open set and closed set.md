A*: open set and closed set


total_cost = cost + heuristic 
CellState {x, y, north, east, south, west, cost}

open.add(startingState)

do while
current state = open.first()
is current state target? if yes, finish
if not: 
  have we visited this node yet? if yes, go next
  if not:
    if north_neighbour is in open: 
      if (north_neighbour.cost > current.cost + 1) north_neighbour.cost = current.cost + 1
    else open 

// Open queue: []
// Closed set: []
// Visited set: []
