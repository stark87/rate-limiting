import http from 'k6/http';
import { sleep } from 'k6';
import { scenario } from 'k6/execution';

export const options = {
    vus: 1,          // 3 virtual users
    duration: '120s', // X time
    iterations: 200, // Total requests across all VUs
};

const BASE_URL = 'http://localhost:8080';

export default function () {
    const params = {
        headers: {
            'x-api-key': 'key-1',
        },
    };


    http.get(`${BASE_URL}/api/v1/data`, params);

    if ((scenario.iterationInTest + 1) % 20 === 0) {
        console.log('Completed 20x of the iterations. Sleeping for 5 seconds...');
        sleep(5);
    }
}