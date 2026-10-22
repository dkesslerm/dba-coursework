# Grid challenges in DBA Lab

**Draft for instructor review. Do not publish yet.** Before distributing this guide, the Grid challenges with energy must be validated against the backend. [Connecting to DBA Lab](guia-api-dba-lab-en.md) explains the shared lifecycle and shows cURL and Java requests.

Challenges with `environmentType: "GRID_2D"` are discovered through `GET /api/v1/challenges`. Each announces its `sensorCodes` and `actionCodes`. The agent uses the capabilities available in the `run` it has started.

## Perceptions

Request each sensor separately with `POST /api/v1/runs/{runId}/perceptions/{sensorId}`. The response contains `sensorId`, `value` and `control`.

| `sensorId` | Fields in `value` |
| --- | --- |
| `grid.position` | `x` and `y`: current position. `x` is the column counted from the left, and `y` is the row counted from the top. |
| `grid.bumpers` | `north`, `east`, `south` and `west`: `true` if the adjacent cell is blocked by an obstacle or the edge of the world. |
| `grid.target` | `x` and `y`: target position. |
| `grid.energy` | In a finite-energy challenge, `mode` is `FINITE` and `remaining` gives the available energy. |

Readings do not move the agent. The laboratory provides these perceptions, not a download of the entire terrain. The images of the first maps in the handout help you recognise the world, while the agent interacts through the sensors of its `run`.

## Movement and result

The agent requests a movement with `POST /api/v1/runs/{runId}/actions` and one of the announced codes: `grid.move.north`, `grid.move.east`, `grid.move.south` or `grid.move.west`. For example, `{"actionCode":"grid.move.east"}` requests a move one cell to the right.

The response includes `actionStatus`, `reasonCode` and `control`. `APPLIED` means the movement was processed; also check `control.runStatus`, because the agent may have reached the target, exhausted its energy or collided with an obstacle. A collision with an obstacle or the edge of the world ends the `run`. The action response does not include the new position or energy balance, so a subsequent reading may be needed if the run remains active.

The Grid challenges in this practice have been configured with finite energy. The specific budget belongs to each challenge's configuration. Reaching the target before exhausting it is part of solving the challenge; route length receives no separate score.
