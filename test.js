import http from 'k6/http';

// export const options = {
//     vus: 3,          // 3 virtual users
//     duration: '30s', // X time
//     iterations: 315, // Total requests across all VUs
// };

export const options = {
    scenarios: {
        fixed: {
            executor: 'per-vu-iterations',
            vus: 3,
            iterations: 105,
        },
    },
};

const users = [
    { key: 'key-1' },
    { key: 'key-2' },
    { key: 'key-3' },
];

const BASE_URL = 'http://localhost:8080';

export default function () {
    // const params = {
    //     headers: {
    //         'x-api-key': 'abc123',
    //     },
    // };

    const user = users[__VU - 1];

    http.get(`${BASE_URL}/api/v1/data`, {
        headers: {
            'x-api-key': `${user.key}`
        }
    });
}