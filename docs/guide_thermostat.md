# Thermostat challenges in DBA Lab

**Draft for instructor review. Do not publish yet.** Read this guide together with [Connecting to DBA Lab](guia-api-dba-lab-en.md), which explains the shared lifecycle, HTTP requests, and cURL and Java examples.

Challenges with `environmentType: "THERMOSTAT"` are discovered through `GET /api/v1/challenges`. The response lists the capabilities available for each challenge. Once a `run` has started, the agent reads the sensors it needs, requests an action and checks the new control state.

## Perceptions

Request each reading separately with `POST /api/v1/runs/{runId}/perceptions/{sensorId}`. The response contains `sensorId`, `value` and `control`. The following table describes the shape of `value`:

| `sensorId` | Fields in `value` |
| --- | --- |
| `thermostat.temperature` | `celsius`: current temperature. |
| `thermostat.limits` | `lower` and `upper`: permitted limits, both inclusive. |
| `thermostat.target` | `celsius`: reference temperature. |

Reading sensors does not advance the simulation. The temperature reading provides the official physical state; action and control responses do not contain a new temperature.

## Actions and result

The thermostat action capabilities are `thermostat.refrigeration.on` and `thermostat.refrigeration.off`, if they appear in the challenge's `actionCodes`. Each applied action sets the refrigeration state and advances the simulation by one step, during which an external disturbance may occur. The thermostat does not use energy.

The goal is to keep the temperature within the limits for the number of steps required by the challenge. The reference temperature helps the agent decide what to do, but reaching it is not in itself the success condition. You do not need to know in advance how many steps remain: the agent continues while `control.runStatus` is `ACTIVE` and stops when DBA Lab reports a terminal result. `control.remainingObjectives` does not count the thermostat's remaining steps.

An action may be `APPLIED` and close the `run` because the temperature became unsafe; also check `control.runStatus` and `control.reasonCode`. If the `run` continues and you need the new temperature, request `thermostat.temperature` again.
