'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import Link from 'next/link';
import { Loader2, MailCheck } from 'lucide-react';
import { forgotPasswordSchema, type ForgotPasswordFormData } from '@/lib/validations/auth';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader } from '@/components/ui/card';
import { AuthHeader } from '@/components/auth/auth-header';
import { ErrorBanner } from '@/components/auth/error-banner';

export default function ForgotPasswordPage() {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [sentTo, setSentTo] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ForgotPasswordFormData>({
    resolver: zodResolver(forgotPasswordSchema),
    mode: 'onTouched',
    defaultValues: {
      email: '',
    },
  });

  const onSubmit = async (data: ForgotPasswordFormData) => {
    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      const res = await fetch('/api/forgot-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });

      if (!res.ok) {
        const json = await res.json();
        setErrorMessage(json.error ?? 'Could not send reset email. Please try again later.');
        return;
      }

      setSentTo(data.email);
    } catch {
      setErrorMessage('Something went wrong. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (sentTo) {
    return (
      <Card className="bg-surface shadow-lg">
        <CardHeader className="space-y-1 text-center">
          <div className="flex justify-center mb-4">
            <MailCheck className="h-12 w-12 text-success" aria-hidden="true" />
          </div>
          <h1 className="text-2xl font-semibold leading-none font-sans">Check your email</h1>
          <CardDescription>
            If an account uses {sentTo}, we&apos;ve sent it a link to reset the password.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <p className="text-sm text-ink-subtle text-center">
              Nothing after a few minutes? Check your spam folder,{' '}
              <button
                type="button"
                onClick={() => setSentTo(null)}
                className="text-primary hover:underline font-medium"
              >
                try a different email
              </button>
              , or ask your administrator to reset it for you.
            </p>
            <Button asChild className="w-full">
              <Link href="/login">Return to sign in</Link>
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="bg-surface shadow-lg">
      <AuthHeader
        title="Reset your password"
        description="Enter your email and we'll send you a link to reset your password"
      />
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          {errorMessage && (
            <ErrorBanner
              message={errorMessage}
              onDismiss={() => setErrorMessage(null)}
            />
          )}

          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              placeholder="Enter your email address"
              autoComplete="email"
              disabled={isSubmitting}
              aria-invalid={!!errors.email}
              aria-describedby={errors.email ? 'email-error' : undefined}
              {...register('email')}
            />
            {errors.email && (
              <p id="email-error" className="text-sm text-error">{errors.email.message}</p>
            )}
          </div>

          <Button type="submit" className="w-full" disabled={isSubmitting}>
            {isSubmitting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Sending...
              </>
            ) : (
              'Send reset link'
            )}
          </Button>

          <div className="text-center text-sm text-ink-subtle">
            Remember your password?{' '}
            <Link
              href="/login"
              className="text-primary hover:underline font-medium"
            >
              Sign in
            </Link>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
