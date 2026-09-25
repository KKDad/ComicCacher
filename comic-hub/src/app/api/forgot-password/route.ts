import { NextResponse, type NextRequest } from 'next/server';
import { GRAPHQL_ENDPOINT } from '@/lib/auth/constants';
import { forgotPasswordSchema } from '@/lib/validations/auth';

export async function POST(request: NextRequest) {
  const body = await request.json();

  const parsed = forgotPasswordSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json(
      { error: parsed.error.issues[0].message },
      { status: 400 },
    );
  }

  const res = await fetch(GRAPHQL_ENDPOINT, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      query: `mutation ForgotPassword($email: String!) {
        forgotPassword(email: $email)
      }`,
      variables: { email: parsed.data.email },
    }),
  });

  const json = await res.json();

  if (json.errors) {
    return NextResponse.json(
      { error: 'Could not send reset email. Please try again later.' },
      { status: 502 },
    );
  }

  // The backend answers true whether or not the address has an account,
  // so the response says nothing about which emails are registered.
  return NextResponse.json({ ok: true });
}
